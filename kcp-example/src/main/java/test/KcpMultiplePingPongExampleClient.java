package test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import kcp.*;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 测试多连接吞吐量
 * Created by JinMiao
 * 2019-06-27.
 */
public class KcpMultiplePingPongExampleClient implements KcpListener {

    public static void main(String[] args) {
        KcpConfig kcpConfig = new KcpConfig();
        kcpConfig.nodelay(true,40,0,true);
        kcpConfig.setSndwnd(256);
        kcpConfig.setRcvwnd(256);
        kcpConfig.setMtu(400);

        ChannelConfig channelConfig = new ChannelConfig(kcpConfig);

        //channelConfig.setFecDataShardCount(10);
        //channelConfig.setFecParityShardCount(3);
        //channelConfig.setAckNoDelay(true);

        //channelConfig.setCrc32Check(true);
        //channelConfig.setTimeoutMillis(10000);

        KcpClient kcpClient = new KcpClient(channelConfig);
        KcpMultiplePingPongExampleClient kcpMultiplePingPongExampleClient = new KcpMultiplePingPongExampleClient();

        int clientNumber = 1000;
        for (int i = 0; i < clientNumber; i++) {
            channelConfig.getKcpConfig().setConv(i);
            kcpClient.connect(new InetSocketAddress("127.0.0.1", 10011), kcpMultiplePingPongExampleClient);
        }
    }

    Timer timer = new Timer();

    @Override
    public void onConnected(Ukcp ukcp) {
        System.out.println(ukcp.getConv());
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                ByteBuf byteBuf = UnpooledByteBufAllocator.DEFAULT.buffer(1004);
                byteBuf.writeInt(1);
                byte[] bytes = new byte[1000];
                byteBuf.writeBytes(bytes);
                ukcp.write(byteBuf);
                byteBuf.release();
            }
        },100,100);
    }

    @Override
    public void handleReceive(ByteBuf byteBuf, Ukcp ukcp) {
        //System.out.println("收到消息");
        //ukcp.writeMessage(byteBuf);
        //int id = byteBuf.getInt(0);
        //if(j-id%10!=0){
        //    System.out.println("id"+id +"  j" +j);
        //}
    }

    @Override
    public void handleException(Throwable ex, Ukcp kcp) {
        ex.printStackTrace();
    }

    @Override
    public void handleClose(Ukcp kcp) {
        System.out.println("连接断开了"+kcp.getConv());
    }


}
