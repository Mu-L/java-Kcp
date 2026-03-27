package test;

import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import kcp.*;

/**
 * 测试多连接吞吐量
 * Created by JinMiao
 * 2019-06-27.
 */
public class KcpMultiplePingPongExampleServer implements KcpListener {

    public static void main(String[] args) {

        KcpMultiplePingPongExampleServer kcpMultiplePingPongExampleServer = new KcpMultiplePingPongExampleServer();
        KcpConfig kcpConfig = new KcpConfig();
        kcpConfig.nodelay(true,40,2,true);
        kcpConfig.setSndwnd(256);
        kcpConfig.setRcvwnd(256);
        kcpConfig.setMtu(400);
        //channelConfig.setFecDataShardCount(10);
        //channelConfig.setFecParityShardCount(3);
        //channelConfig.setAckNoDelay(true);

        ChannelConfig channelConfig = new ChannelConfig(kcpConfig);
        channelConfig.setUseConvChannel(true);
        //channelConfig.setCrc32Check(true);
        channelConfig.setTimeoutMillis(10000);
        KcpServer kcpServer = new KcpServer();
        kcpServer.init(kcpMultiplePingPongExampleServer, channelConfig, 10011);
    }


    @Override
    public void onConnected(Ukcp ukcp) {
        System.out.println("有连接进来" + ukcp.user().getRemoteAddress() +"  conv: "+ ukcp.getConv());
    }

    //int i = 0;
    //
    //long start = System.currentTimeMillis();

    @Override
    public void handleReceive(ByteBuf buf, Ukcp kcp) {
        //i++;
        //long now = System.currentTimeMillis();
        //if(now-start>1000){
        //    System.out.println("收到消息 time: "+(now-start) +"  message :" +i);
        //    start = now;
        //    i=0;
        //}
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
        System.out.println("连接断开了"+kcp.getConv()+" "+System.currentTimeMillis());
    }
}