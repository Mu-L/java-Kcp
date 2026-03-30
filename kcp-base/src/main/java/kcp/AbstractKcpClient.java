package kcp;

import com.backblaze.erasure.fec.Fec;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
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
import threadPool.IMessageExecutor;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Kcp Client 模板
 * <p>
 * Created in 2026/3/27
 *
 * @author JinMiao
 * @author <a href="https://github.com/tzfun/">tzfun</a>
 */
public abstract class AbstractKcpClient {

    private static final Logger logger = LoggerFactory.getLogger(AbstractKcpClient.class);

    public static class TimerThreadFactory implements ThreadFactory {
        private final AtomicInteger timeThreadName = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "KcpClientTimerThread " + timeThreadName.addAndGet(1));
        }
    }

    protected final ChannelConfig channelConfig;

    /**
     * 客户端的连接集合
     */
    private volatile IChannelManager channelManager;
    private volatile Bootstrap bootstrap;
    private volatile EventLoopGroup group;
    private volatile Timer timer;

    public AbstractKcpClient(ChannelConfig channelConfig) {
        this.channelConfig = channelConfig;
        init();
    }

    protected void init() {
        if (channelConfig.isUseConvChannel()) {
            int convIndex = 0;
            if (channelConfig.getFecAdapt() != null) {
                convIndex += Fec.fecHeaderSizePlus2;
            }
            channelManager = new ClientConvChannelManager(convIndex);
        } else {
            channelManager = new ClientAddressChannelManager();
        }
        timer = new HashedWheelTimer(new TimerThreadFactory(), 1, TimeUnit.MILLISECONDS);
        bootstrap = new Bootstrap();
        initBootstrapGroup();

        bootstrap.handler(new ChannelInitializer<DatagramChannel>() {
            @Override
            protected void initChannel(DatagramChannel ch) {
                AbstractKcpClient.this.initChannel(ch);
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    protected void initBootstrapGroup() {
        group = channelConfig.getNettyBootstrapGroup();
        Class<? extends DatagramChannel> channelClass = channelConfig.getNettyBootstrapChannelClass();

        //  调用层未传入外部管理的线程组，默认创建一个
        if (group == null) {
            boolean epoll = Epoll.isAvailable();
            boolean kqueue = KQueue.isAvailable();
            int cpuNum = Runtime.getRuntime().availableProcessors();
            logger.info("Epoll available: {}, KQueue available: {}", epoll, kqueue);

            if (epoll) {
                group = new EpollEventLoopGroup(cpuNum);
                channelClass = EpollDatagramChannel.class;
            } else if (kqueue) {
                group = new KQueueEventLoopGroup(cpuNum);
                channelClass = KQueueDatagramChannel.class;
            } else {
                group = new NioEventLoopGroup(cpuNum);
                channelClass = NioDatagramChannel.class;
            }
        }

        bootstrap.channel(channelClass);
        bootstrap.group(group);
    }

    protected void initChannel(DatagramChannel ch) {
        ChannelPipeline cp = ch.pipeline();
        if (channelConfig.isCrc32Check()) {
            Crc32Encode crc32Encode = new Crc32Encode();
            Crc32Decode crc32Decode = new Crc32Decode();
            cp.addLast(crc32Encode);
            cp.addLast(crc32Decode);
        }
        cp.addLast(new ClientChannelHandler(channelManager));
    }

    public synchronized boolean isStarted() {
        return group != null && !group.isShutdown();
    }

    public synchronized void stop() {
        if (!isStarted()) {
            return;
        }

        logger.info("Prepare to stop the kcp client...");

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

        logger.info("Kcp client stopped");
    }


    /**
     * 重连接口
     * <p>
     * 使用旧的kcp对象，出口ip和端口替换为新的
     * <p>
     * 在4G切换为wifi等场景使用
     */
    public void reconnect(Ukcp ukcp) {
        if (!(channelManager instanceof ClientConvChannelManager)) {
            throw new UnsupportedOperationException("reconnect can only be used in convChannel");
        }
        ukcp.getiMessageExecutor().execute(() -> {
            User user = ukcp.user();
            user.getChannel().close();
            InetSocketAddress localAddress = new InetSocketAddress(0);
            ChannelFuture channelFuture = bootstrap.connect(user.getRemoteAddress(), localAddress);
            user.setChannel(channelFuture.channel());
        });
    }

    public Ukcp connect(InetSocketAddress localAddress, InetSocketAddress remoteAddress, KcpListener kcpListener) {
        if (localAddress == null) {
            localAddress = new InetSocketAddress(0);
        }
        ChannelFuture channelFuture = bootstrap.connect(remoteAddress, localAddress);

        //= bootstrap.bind(localAddress);
        ChannelFuture sync = channelFuture.syncUninterruptibly();
        NioDatagramChannel channel = (NioDatagramChannel) sync.channel();
        localAddress = channel.localAddress();

        User user = new User(channel, remoteAddress, localAddress);
        IMessageExecutor executor = channelConfig.getMessageExecutorPool().getIMessageExecutor();
        KcpOutput kcpOutput = new KcpOutPutImp();

        Ukcp ukcp = new Ukcp(kcpOutput, kcpListener, executor, channelConfig, channelManager);
        ukcp.user(user);

        channelManager.add(localAddress, ukcp, null);
        executor.execute(() -> {
            try {
                ukcp.getKcpListener().onConnected(ukcp);
            } catch (Throwable throwable) {
                ukcp.getKcpListener().handleException(throwable, ukcp);
            }
        });

        ScheduleTask scheduleTask = new ScheduleTask(executor, ukcp, timer);
        timer.newTimeout(scheduleTask, ukcp.getInterval(), TimeUnit.MILLISECONDS);
        return ukcp;
    }

    public Ukcp connect(InetSocketAddress remoteAddress, KcpListener kcpListener) {
        return connect(null, remoteAddress, kcpListener);
    }

    public IChannelManager getChannelManager() {
        return channelManager;
    }
}
