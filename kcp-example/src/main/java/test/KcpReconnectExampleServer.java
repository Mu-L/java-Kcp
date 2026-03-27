package test;

import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import kcp.*;

/**
 * 重连测试服务器
 * Created by JinMiao
 * 2019-06-27.
 */
public class KcpReconnectExampleServer implements KcpListener {

    public static void main(String[] args) {

        KcpReconnectExampleServer kcpRttExampleServer = new KcpReconnectExampleServer();
        KcpConfig kcpConfig = new KcpConfig();
        kcpConfig.nodelay(true,40,2,true);
        kcpConfig.setSndwnd(1024);
        kcpConfig.setRcvwnd(1024);
        kcpConfig.setMtu(1400);
        //channelConfig.setFecDataShardCount(10);
        //channelConfig.setFecParityShardCount(3);
        //channelConfig.setAckNoDelay(true);
        //channelConfig.setCrc32Check(true);

        ChannelConfig channelConfig = new ChannelConfig(kcpConfig);
        channelConfig.setUseConvChannel(true);
        channelConfig.setTimeoutMillis(10000);

        KcpServer.createStarted(channelConfig, kcpRttExampleServer, 10021);
    }


    @Override
    public void onConnected(Ukcp ukcp) {
        System.out.println("有连接进来" + Thread.currentThread().getName() + ukcp.user().getRemoteAddress());
    }

    int i = 0;

    long start = System.currentTimeMillis();

    @Override
    public void handleReceive(ByteBuf buf, Ukcp kcp) {
        i++;
        long now = System.currentTimeMillis();
        if(now-start>1000){
            System.out.println("收到消息 time: "+(now-start) +"  message :" +i);
            start = now;
            i=0;
        }
        kcp.write(buf);
    }

    @Override
    public void handleException(Throwable ex, Ukcp kcp) {
        ex.printStackTrace();
    }

    @Override
    public void handleClose(Ukcp kcp) {
        System.out.println(Snmp.snmp.toString());
        Snmp.snmp= new Snmp();
        System.out.println("连接断开了");
    }
}