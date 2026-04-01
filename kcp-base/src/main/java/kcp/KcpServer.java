package kcp;

/**
 * Created by JinMiao
 * 2018/9/20.
 */
public class KcpServer extends AbstractKcpServer {

    /**
     * 创建一个未启动的 KCP Server，需要调用 {@link #start(int...)} 方法启动本服务
     *
     * @param channelConfig KCP协议相关配置
     * @param kcpListener   KCP消息监听器
     */
    public KcpServer(ChannelConfig channelConfig, KcpListener kcpListener) {
        super(channelConfig, kcpListener);
    }

    /**
     * 创建一个 KcpServer 并启动服务
     *
     * @param channelConfig KCP协议相关配置
     * @param kcpListener   KCP消息监听器
     * @param ports         服务监听端口
     * @return {@link KcpServer} 实例
     */
    public static KcpServer createStarted(ChannelConfig channelConfig, KcpListener kcpListener, int... ports) {
        KcpServer kcpServer = new KcpServer(channelConfig, kcpListener);
        kcpServer.start(ports);
        return kcpServer;
    }

    /**
     * Deprecated. 请使用 {@link AbstractKcpServer#start(int...)} 或 {@link #createStarted(ChannelConfig, KcpListener, int...)} 方法
     */
    @Deprecated
    public void init(KcpListener kcpListener, ChannelConfig channelConfig, int... ports) {
        start(ports);
    }

}
