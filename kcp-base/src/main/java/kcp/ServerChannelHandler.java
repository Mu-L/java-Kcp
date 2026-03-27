package kcp;

import com.backblaze.erasure.fec.Fec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.Timer;
import threadPool.IMessageExecutor;

import java.util.concurrent.TimeUnit;

/**
 * Created by JinMiao
 * 2018/9/20.
 */
public class ServerChannelHandler extends ChannelInboundHandlerAdapter {

    private final IChannelManager channelManager;
    private final ChannelConfig channelConfig;
    private final KcpListener kcpListener;
    private final Timer timer;

    private Ukcp channelUkcp;

    public ServerChannelHandler(IChannelManager channelManager, ChannelConfig channelConfig, KcpListener kcpListener,Timer timer) {
        this.channelManager = channelManager;
        this.channelConfig = channelConfig;
        this.kcpListener = kcpListener;
        this.timer = timer;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (channelUkcp == null) {
            kcpListener.handleException(cause, null);
        } else {
            //  如果此异常发生在读取消息之后，会缓存 Ukcp 对象，在已知连接发生异常时尽量将异常信息通知给业务层
            channelUkcp.getiMessageExecutor().execute(() -> kcpListener.handleException(cause, channelUkcp));
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object object) {
        DatagramPacket msg = (DatagramPacket) object;
        Ukcp ukcp = channelManager.get(msg);
        ByteBuf byteBuf = msg.content();
        channelUkcp = ukcp;

        if (ukcp != null) {
            User user = ukcp.user();
            //每次收到消息重绑定地址
            user.setRemoteAddress(msg.sender());
            ukcp.read(byteBuf);
            return;
        }

        //如果是新连接第一个包的sn必须为0
        int sn = getSn(byteBuf,channelConfig);
        if(sn!=0){
            msg.release();
            return;
        }
        IMessageExecutor executor = channelConfig.getMessageExecutorPool().getIMessageExecutor();
        KcpOutput kcpOutput = new KcpOutPutImp();
        Ukcp newUkcp = new Ukcp(kcpOutput, kcpListener, executor, channelConfig, channelManager);

        newUkcp.user(new User(ctx.channel(), msg.sender(), msg.recipient()));
        channelManager.New(msg.sender(), newUkcp, msg);

        executor.execute(() -> {
            try {
                newUkcp.getKcpListener().onConnected(newUkcp);
            } catch (Throwable throwable) {
                newUkcp.getKcpListener().handleException(throwable, newUkcp);
            }
        });

        newUkcp.read(byteBuf);

        ScheduleTask scheduleTask = new ScheduleTask(executor, newUkcp, timer);
        timer.newTimeout(scheduleTask,newUkcp.getInterval(), TimeUnit.MILLISECONDS);
    }

    protected int getSn(ByteBuf byteBuf,ChannelConfig channelConfig){
        int headerSize = 0;
        if(channelConfig.getFecAdapt()!=null){
            headerSize+= Fec.fecHeaderSizePlus2;
        }

        return byteBuf.getIntLE(byteBuf.readerIndex()+Kcp.IKCP_SN_OFFSET+headerSize);
    }

}
