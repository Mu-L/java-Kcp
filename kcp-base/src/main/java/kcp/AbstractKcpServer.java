package kcp;

import com.backblaze.erasure.fec.Fec;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Kcp Server模板
 * <p>
 * Created in 2026/3/27
 *
 * @author JinMiao
 * @author <a href="https://github.com/tzfun/">tzfun</a>
 */
public abstract class AbstractKcpServer {
    private static final Logger logger = LoggerFactory.getLogger(AbstractKcpServer.class);

    public static class TimerThreadFactory implements ThreadFactory {
        private final AtomicInteger timeThreadName = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "KcpServerTimerThread " + timeThreadName.addAndGet(1));
        }
    }

    protected final ChannelConfig channelConfig;
    protected final KcpListener kcpListener;

    protected volatile Bootstrap bootstrap;
    protected volatile EventLoopGroup group;
    protected volatile List<Channel> localAddressList = new Vector<>();
    protected volatile IChannelManager channelManager;
    protected volatile Timer timer;

    /**
     * 创建一个未启动的 KCP Server，需要调用 {@link #start(int...)} 方法启动本服务
     *
     * @param channelConfig KCP协议相关配置
     * @param kcpListener   KCP消息监听器
     */
    public AbstractKcpServer(ChannelConfig channelConfig, KcpListener kcpListener) {
        this.channelConfig = channelConfig;
        this.kcpListener = kcpListener;
    }

    /**
     * 启动 KCP 服务
     *
     * @param ports 服务绑定的端口，可以传多个值
     * @throws IllegalStateException 如果本服务已经启动，会抛出此异常
     */
    public synchronized void start(int... ports) throws IllegalStateException {
        if (isStarted()) {
            throw new IllegalStateException("Kcp server is already started");
        }
        if (channelConfig.isUseConvChannel()) {
            int convIndex = 0;
            if (channelConfig.getFecAdapt() != null) {
                convIndex += Fec.fecHeaderSizePlus2;
            }
            channelManager = new ServerConvChannelManager(convIndex);
        } else {
            channelManager = new ServerAddressChannelManager();
        }
        timer = new HashedWheelTimer(new TimerThreadFactory(), 1, TimeUnit.MILLISECONDS);
        bootstrap = new Bootstrap();

        boolean epoll = Epoll.isAvailable();
        boolean kqueue = KQueue.isAvailable();
        logger.info("Epoll available: {}, KQueue available: {}", epoll, kqueue);

        int cpuNum = Runtime.getRuntime().availableProcessors();
        int bindTimes = 1;
        if (epoll || kqueue) {
            //  ADD SO_REUSEPORT
            //  https://www.jianshu.com/p/61df929aa98b
            bootstrap.option(EpollChannelOption.SO_REUSEPORT, true);
            bindTimes = cpuNum;
        }

        initBootstrapGroup(epoll, kqueue, cpuNum, ports.length);
        bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) {
                AbstractKcpServer.this.initChannel(ch);
            }
        });
//        bootstrap.option(ChannelOption.SO_RCVBUF, 10*1024*1024);
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);

        for (int port : ports) {
            for (int i = 0; i < bindTimes; i++) {
                ChannelFuture channelFuture = bootstrap.bind(port);

                logger.info("Kcp server start up on {}, number {}", port, i);

                Channel channel = channelFuture.channel();
                localAddressList.add(channel);
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    protected void initBootstrapGroup(boolean epoll, boolean kqueue, int cpuNum, int portNum) {
        group = channelConfig.getNettyBootstrapGroup();
        Class<? extends DatagramChannel> channelClass = channelConfig.getNettyBootstrapChannelClass();

        //  调用层未传入外部管理的线程组，默认创建一个
        if (group == null) {
            if (epoll) {
                group = new EpollEventLoopGroup(cpuNum);
                channelClass = EpollDatagramChannel.class;
            } else if (kqueue) {
                group = new KQueueEventLoopGroup(cpuNum);
                channelClass = KQueueDatagramChannel.class;
            } else {
                group = new NioEventLoopGroup(portNum);
                channelClass = NioDatagramChannel.class;
            }
        }

        bootstrap.channel(channelClass);
        bootstrap.group(group);
    }

    public synchronized void stop() {
        if (!isStarted()) {
            return;
        }
        logger.info("Prepare to stop the kcp server...");
        //  Close port binding
        localAddressList.forEach(ChannelOutboundInvoker::close);
        localAddressList.clear();

        //  Close all kcp channel
        channelManager.getAll().forEach(Ukcp::close);
        channelManager = null;

        //  Stop message executor pool
        channelConfig.getMessageExecutorPool().stop();

        //  Stop timer
        timer.stop();
        timer = null;

        //  Stop boss group
        if (channelConfig.getNettyBootstrapGroup() == null) {
            //  只有内部创建的group才需要关闭，外部传入的group交由外部管理其生命周期
            group.shutdownGracefully();
        }
        group = null;

        logger.info("Kcp server stopped");
    }

    public synchronized boolean isStarted() {
        return group != null && !group.isShutdown();
    }

    protected void initChannel(Channel ch) {
        ServerChannelHandler handler = new ServerChannelHandler(channelManager, channelConfig, kcpListener, timer);
        ChannelPipeline cp = ch.pipeline();
        if (channelConfig.isCrc32Check()) {
            Crc32Encode crc32Encode = new Crc32Encode();
            Crc32Decode crc32Decode = new Crc32Decode();
            //  这里的crc32放在eventloop网络线程处理的，以后内核有丢包可以优化到单独的一个线程处理
            cp.addLast(crc32Encode);
            cp.addLast(crc32Decode);
        }
        cp.addLast(handler);
    }

    public IChannelManager getChannelManager() {
        return channelManager;
    }
}
