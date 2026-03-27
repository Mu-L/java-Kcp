package test.TpsTest;

import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import kcp.*;
import test.*;

/**
 *
 * Created by JinMiao
 * 2019-06-25.
 */
public class KcpGameTestServer implements KcpListener
{

    private GameTestRoomManager roomManager;


    public static void main(String[] args) {
        KcpGameTestServer kcpGameTestServer = new KcpGameTestServer();
        KcpConfig kcpConfig = new KcpConfig();
        kcpConfig.nodelay(true,40,2,true);
        kcpConfig.setSndwnd(300);
        kcpConfig.setRcvwnd(300);
        kcpConfig.setMtu(500);
        //channelConfig.setFecDataShardCount(10);
        //channelConfig.setFecParityShardCount(3);
        kcpConfig.setAckNoDelay(false);

        ChannelConfig channelConfig = new ChannelConfig(kcpConfig);
        channelConfig.setCrc32Check(true);
        channelConfig.setTimeoutMillis(10000);
        KcpServer kcpServer = new KcpServer();
        kcpServer.init(kcpGameTestServer, channelConfig, 10019);

        kcpGameTestServer.roomManager = new GameTestRoomManager();


        TimerThreadPool.scheduleWithFixedDelay(() -> {
            try {
                long inSegs = Snmp.snmp.InSegs.longValue();
                if(inSegs==0){
                    inSegs = 1;
                }
                System.out.println("每秒收包"+ (Snmp.snmp.InBytes.longValue()/1024.0/1024.0*8.0)+" M"+" 丢包率 "+((double)Snmp.snmp.LostSegs.longValue()/inSegs));
                System.out.println("每秒发包"+ (Snmp.snmp.OutBytes.longValue()/1024.0/1024.0*8.0)+" M");
                System.out.println("房间数 "+kcpGameTestServer.roomManager.getRooms().size());
                int playerCount = 0;
                for (TestRoom value : kcpGameTestServer.roomManager.getRooms().values()) {
                    playerCount+=value.size();
                }
                System.out.println("总人数数 "+playerCount);
                System.out.println(Snmp.snmp.toString());
                System.out.println();
                Snmp.snmp = new Snmp();

            }catch (Exception e){
                e.printStackTrace();
            }
        },1000);
    }







    @Override
    public void onConnected(Ukcp ukcp) {
        System.out.println("有连接进来"+ukcp.user());
        TpsChannelServerCache tpsChannelServerCache = new TpsChannelServerCache();
        ukcp.user().setCache(tpsChannelServerCache);
        roomManager.addClient(ukcp);
    }

    @Override
    public void handleReceive(ByteBuf byteBuf, Ukcp ukcp) {
        //System.out.println("收到消息"+ukcp.user());
        TpsChannelServerCache tpsChannelServerCache = ukcp.user().getCache();
        int packId = byteBuf.readInt();
        tpsChannelServerCache.addPackId(packId);
    }

    @Override
    public void handleException(Throwable ex, Ukcp ukcp) {
        ex.printStackTrace();
    }

    @Override
    public void handleClose(Ukcp ukcp) {
        roomManager.remove(ukcp);
        System.out.println("连接断开了"+ukcp.user().getRemoteAddress());
    }
}
