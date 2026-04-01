package kcp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by JinMiao
 * 2019-06-26.
 */
public class ClientChannelHandler extends ChannelInboundHandlerAdapter {
    static final Logger logger = LoggerFactory.getLogger(ClientChannelHandler.class);

    protected final IChannelManager channelManager;

    protected Ukcp channelUkcp;

    public ClientChannelHandler(IChannelManager channelManager) {
        this.channelManager = channelManager;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (channelUkcp == null) {
            logger.error("exceptionCaught", cause);
        } else {
            //  如果此异常发生在读取消息之后，会缓存 Ukcp 对象，在已知连接发生异常时尽量将异常信息通知给业务层
            channelUkcp.getiMessageExecutor().execute(() -> channelUkcp.getKcpListener().handleException(cause, channelUkcp));
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object object) {
        DatagramPacket msg = (DatagramPacket) object;
        Ukcp ukcp = this.channelManager.get(msg);
        channelUkcp = ukcp;
        if (ukcp != null) {
            ukcp.read(msg.content());
        }
    }
}
