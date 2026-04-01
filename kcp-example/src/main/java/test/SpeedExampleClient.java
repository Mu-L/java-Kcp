package test;

import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import kcp.*;
import threadPool.disruptor.DisruptorExecutorPool;

import java.net.InetSocketAddress;

/**
 * Created by JinMiao
 * 2020/12/23.
 */
public class SpeedExampleClient implements KcpListener {


    public SpeedExampleClient() {
    }

    public static void main(String[] args) {
        KcpConfig kcpConfig = new KcpConfig();
        kcpConfig.nodelay(true,30,2,true);
        kcpConfig.setSndwnd(2048);
        kcpConfig.setRcvwnd(2048);
        kcpConfig.setMtu(1400);
        kcpConfig.setAckNoDelay(true);
        kcpConfig.setConv(55);

        DisruptorExecutorPool executorPool = new DisruptorExecutorPool(Runtime.getRuntime().availableProcessors() / 2);

        ChannelConfig channelConfig = new ChannelConfig(kcpConfig, executorPool);

        //channelConfig.setFecDataShardCount(10);
        //channelConfig.setFecParityShardCount(3);
        channelConfig.setCrc32Check(false);
        channelConfig.setWriteBufferSize(kcpConfig.getMtu()*300000);
        KcpClient kcpClient = new KcpClient(channelConfig);

        SpeedExampleClient speedExampleClient = new SpeedExampleClient();
        kcpClient.connect(new InetSocketAddress("127.0.0.1",20004),speedExampleClient);

    }
    private static final int messageSize = 2048;
    private long start = System.currentTimeMillis();

    @Override
    public void onConnected(Ukcp ukcp) {
        new Thread(() -> {
            for(;;){
                long now =System.currentTimeMillis();
                if(now-start>=1000){
                    System.out.println("耗时 :" +(now-start) +" 发送数据: " +(Snmp.snmp.OutBytes.doubleValue()/1024.0/1024.0)+"MB"+" 有效数据: "+Snmp.snmp.BytesSent.doubleValue()/1024.0/1024.0+" MB");
                    System.out.println(Snmp.snmp.toString());
                    Snmp.snmp = new Snmp();
                    start=now;
                }
                ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(messageSize);
                byteBuf.writeBytes(new byte[messageSize]);
                if(!ukcp.write(byteBuf)){
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                byteBuf.release();
            }
        }).start();
    }

    @Override
    public void handleReceive(ByteBuf byteBuf, Ukcp ukcp) {
    }

    @Override
    public void handleException(Throwable ex, Ukcp kcp)
    {
        ex.printStackTrace();
    }

    @Override
    public void handleClose(Ukcp kcp) {
    }
}
