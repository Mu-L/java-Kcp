package kcp;

import com.backblaze.erasure.FecAdapt;
import threadPool.IMessageExecutorPool;
import threadPool.netty.NettyMessageExecutorPool;

/**
 * Created by JinMiao
 * 2018/9/20.
 */
public class ChannelConfig {
    public static final int crc32Size = 4;

    /**
     * The underlying KCP protocol configuration.
     */
    protected final KcpConfig kcpConfig;

    /**
     * 处理kcp消息接收和发送的线程池
     */
    protected IMessageExecutorPool executorPool;

    /**
     * 超时时间 超过一段时间没收到消息断开连接
     */
    protected long timeoutMillis;

    /**
     * FEC(Forward Error Correction) 前向纠错适配器
     */
    protected FecAdapt fecAdapt;

    /**
     * 发送包立即调用flush 延迟低一些  cpu增加  如果interval值很小 建议关闭该参数
     */
    protected boolean fastFlush = true;

    /**
     * 是否开启 crc32 校验
     */
    protected boolean crc32Check = false;

    /**
     * 接收窗口大小(字节 -1不限制)
     */
    protected int readBufferSize = -1;
    /**
     * 发送窗口大小(字节 -1不限制)
     */
    protected int writeBufferSize = -1;

    /**
     * 使用conv确定一个channel 还是使用 socketAddress确定一个channel
     */
    protected boolean useConvChannel = false;

    public ChannelConfig() {
        this(null, null);
    }

    public ChannelConfig(KcpConfig kcpConfig) {
        this(kcpConfig, null);
    }

    public ChannelConfig(KcpConfig kcpConfig, IMessageExecutorPool executorPool) {
        this.kcpConfig = kcpConfig == null
                ? new KcpConfig()
                : kcpConfig;
        this.executorPool = executorPool == null
                ? new NettyMessageExecutorPool(Runtime.getRuntime().availableProcessors())
                : executorPool;
    }

    public KcpConfig getKcpConfig() {
        return kcpConfig;
    }

    /**
     * Deprecated. 请使用 {@link KcpConfig#nodelay(boolean, int, int, boolean)}
     */
    @Deprecated
    public void nodelay(boolean nodelay, int interval, int resend, boolean nc) {
        kcpConfig.nodelay(nodelay, interval, resend, nc);
    }

    public int getReadBufferSize() {
        return readBufferSize;
    }

    public void setReadBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
    }

    /**
     * Deprecated. 使用 {@link #getMessageExecutorPool()}
     *
     * @return {@link IMessageExecutorPool}
     */
    @Deprecated
    public IMessageExecutorPool getiMessageExecutorPool() {
        return executorPool;
    }

    public IMessageExecutorPool getMessageExecutorPool() {
        return executorPool;
    }

    /**
     * Deprecated. 请使用构造函数传入线程池
     */
    @Deprecated
    public void setiMessageExecutorPool(IMessageExecutorPool iMessageExecutorPool) {
        if (this.executorPool != null) {
            this.executorPool.stop();
        }
        this.executorPool = iMessageExecutorPool;
    }

    /**
     * Deprecated. 请使用 {@link #getKcpConfig()} 获取KCP基础配置对象，然后使用{@link KcpConfig#isNodelay()}方法
     */
    @Deprecated
    public boolean isNodelay() {
        return kcpConfig.isNodelay();
    }

    /**
     * Deprecated. 请使用 {@link #getKcpConfig()} 获取KCP基础配置对象，然后使用{@link KcpConfig#getConv()}方法
     */
    @Deprecated
    public int getConv() {
        return kcpConfig.getConv();
    }

    /**
     * Deprecated. 请使用 {@link #getKcpConfig()} 获取KCP基础配置对象，然后使用{@link KcpConfig#setConv}方法
     */
    @Deprecated
    public void setConv(int conv) {
        kcpConfig.setConv(conv);
    }

    /**
     * Deprecated. 请使用 {@link #getKcpConfig()} 获取KCP基础配置对象，然后使用{@link KcpConfig#getInterval}方法
     */
    @Deprecated
    public int getInterval() {
        return kcpConfig.getInterval();
    }

    /**
     * Deprecated. 请使用 {@link #getKcpConfig()} 获取KCP基础配置对象，然后使用{@link KcpConfig#getFastresend}方法
     */
    @Deprecated
    public int getFastresend() {
        return kcpConfig.getFastresend();
    }

    /**
     * Deprecated. 请使用 {@link #getKcpConfig()} 获取KCP基础配置对象，然后使用{@link KcpConfig#isNocwnd}方法
     */
    @Deprecated
    public boolean isNocwnd() {
        return kcpConfig.isNocwnd();
    }

    /**
     * Deprecated. 请使用 {@link #getKcpConfig()} 获取KCP基础配置对象，然后使用{@link KcpConfig#getSndwnd}方法
     */
    @Deprecated
    public int getSndwnd() {
        return kcpConfig.getSndwnd();
    }

    /**
     * Deprecated. 请使用 {@link #getKcpConfig()} 获取KCP基础配置对象，然后使用{@link KcpConfig#setSndwnd}方法
     */
    @Deprecated
    public void setSndwnd(int sndwnd) {
        kcpConfig.setSndwnd(sndwnd);
    }

    /**
     * Deprecated. 请使用 {@link #getKcpConfig()} 获取KCP基础配置对象，然后使用{@link KcpConfig#getRcvwnd}方法
     */
    @Deprecated
    public int getRcvwnd() {
        return kcpConfig.getRcvwnd();
    }

    /**
     * Deprecated. 请使用 {@link #getKcpConfig()} 获取KCP基础配置对象，然后使用{@link KcpConfig#setRcvwnd}方法
     */
    @Deprecated
    public void setRcvwnd(int rcvwnd) {
        kcpConfig.setRcvwnd(rcvwnd);
    }

    /**
     * Deprecated. 请使用 {@link #getKcpConfig()} 获取KCP基础配置对象，然后使用{@link KcpConfig#getMtu}方法
     */
    @Deprecated
    public int getMtu() {
        return kcpConfig.getMtu();
    }

    /**
     * Deprecated. 请使用 {@link #getKcpConfig()} 获取KCP基础配置对象，然后使用{@link KcpConfig#setMtu}方法
     */
    @Deprecated
    public void setMtu(int mtu) {
        kcpConfig.setMtu(mtu);
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Deprecated. 请使用 {@link #getKcpConfig()} 获取KCP基础配置对象，然后使用{@link KcpConfig#isStream}方法
     */
    @Deprecated
    public boolean isStream() {
        return kcpConfig.isStream();
    }

    /**
     * Deprecated. 请使用 {@link #getKcpConfig()} 获取KCP基础配置对象，然后使用{@link KcpConfig#setStream}方法
     */
    @Deprecated
    public void setStream(boolean stream) {
        kcpConfig.setStream(stream);
    }

    public FecAdapt getFecAdapt() {
        return fecAdapt;
    }

    public void setFecAdapt(FecAdapt fecAdapt) {
        this.fecAdapt = fecAdapt;
    }

    /**
     * Deprecated. 请使用 {@link #getKcpConfig()} 获取KCP基础配置对象，然后使用{@link KcpConfig#isAckNoDelay}方法
     */
    @Deprecated
    public boolean isAckNoDelay() {
        return kcpConfig.isAckNoDelay();
    }

    /**
     * Deprecated. 请使用 {@link #getKcpConfig()} 获取KCP基础配置对象，然后使用{@link KcpConfig#setAckNoDelay}方法
     */
    @Deprecated
    public void setAckNoDelay(boolean ackNoDelay) {
        kcpConfig.setAckNoDelay(ackNoDelay);
    }

    public boolean isFastFlush() {
        return fastFlush;
    }

    public void setFastFlush(boolean fastFlush) {
        this.fastFlush = fastFlush;
    }

    public boolean isCrc32Check() {
        return crc32Check;
    }

    /**
     * Deprecated. 请使用 {@link #getKcpConfig()} 获取KCP基础配置对象，然后使用{@link KcpConfig#getAckMaskSize}方法
     */
    @Deprecated
    public int getAckMaskSize() {
        return kcpConfig.getAckMaskSize();
    }

    /**
     * Deprecated. 请使用 {@link #getKcpConfig()} 获取KCP基础配置对象，然后使用{@link KcpConfig#setAckMaskSize}方法
     */
    @Deprecated
    public void setAckMaskSize(int ackMaskSize) {
        kcpConfig.setAckMaskSize(ackMaskSize);
    }

    public void setCrc32Check(boolean crc32Check) {
        this.crc32Check = crc32Check;
    }

    public boolean isUseConvChannel() {
        return useConvChannel;
    }

    public int getWriteBufferSize() {
        return writeBufferSize;
    }

    public void setWriteBufferSize(int writeBufferSize) {
        this.writeBufferSize = writeBufferSize;
    }

    public void setUseConvChannel(boolean useConvChannel) {
        this.useConvChannel = useConvChannel;
    }
}
