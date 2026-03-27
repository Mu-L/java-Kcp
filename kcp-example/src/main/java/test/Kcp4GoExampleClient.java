package test;

import com.backblaze.erasure.FecAdapt;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import kcp.*;

import java.net.InetSocketAddress;

/**
 * 与go版本兼容的客户端
 * Created by JinMiao
 * 2019/11/29.
 */
public class Kcp4GoExampleClient implements KcpListener {

    public static void main(String[] args) {
        KcpConfig kcpConfig = new KcpConfig();
        kcpConfig.nodelay(true,40,2,true);
        kcpConfig.setSndwnd(1024);
        kcpConfig.setRcvwnd(1024);
        kcpConfig.setMtu(1400);
        kcpConfig.setAckNoDelay(false);
        kcpConfig.setAckMaskSize(0);

        ChannelConfig channelConfig = new ChannelConfig(kcpConfig);

        channelConfig.setFecAdapt(new FecAdapt(10,3));
        //channelConfig.setTimeoutMillis(10000);

        //禁用参数
        channelConfig.setCrc32Check(false);

        KcpClient kcpClient = new KcpClient();
        kcpClient.init(channelConfig);


        Kcp4GoExampleClient kcpGoExampleClient = new Kcp4GoExampleClient();
        Ukcp ukcp = kcpClient.connect(new InetSocketAddress("127.0.0.1", 10000), channelConfig, kcpGoExampleClient);
        String msg = "hello!!!!!11111111111111111111111111";
        byte[] bytes = msg.getBytes();
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.ioBuffer(bytes.length);
        byteBuf.writeBytes(bytes);
        ukcp.write(byteBuf);

    }
    @Override
    public void onConnected(Ukcp ukcp) {

    }

    @Override
    public void handleReceive(ByteBuf byteBuf, Ukcp ukcp) {

    }

    @Override
    public void handleException(Throwable ex, Ukcp ukcp) {

    }

    @Override
    public void handleClose(Ukcp ukcp) {

    }
}
