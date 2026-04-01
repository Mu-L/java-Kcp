package kcp;

import java.net.InetSocketAddress;

/**
 * kcp客户端
 * Created by JinMiao
 * 2019-06-26.
 */
public class KcpClient extends AbstractKcpClient {

    public KcpClient(ChannelConfig channelConfig) {
        super(channelConfig);
    }

    /**
     * Deprecated. 此方法无需再调用，构造实例时会自动初始化
     */
    @Deprecated
    public void init(ChannelConfig channelConfig) {
    }

    /**
     * Deprecated. 请使用 {@link #connect(InetSocketAddress, InetSocketAddress, KcpListener)}
     */
    @Deprecated
    public Ukcp connect(InetSocketAddress localAddress, InetSocketAddress remoteAddress, ChannelConfig channelConfig, KcpListener kcpListener) {
        return super.connect(localAddress, remoteAddress, kcpListener);
    }

    /**
     * Deprecated. 请使用  {@link #connect(InetSocketAddress, KcpListener)}
     */
    @Deprecated
    public Ukcp connect(InetSocketAddress remoteAddress, ChannelConfig channelConfig, KcpListener kcpListener) {
        return super.connect(remoteAddress, kcpListener);
    }
}
