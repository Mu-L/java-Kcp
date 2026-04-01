package kcp;

import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threadPool.IMessageExecutor;
import threadPool.ITask;

import java.util.concurrent.TimeUnit;

/**
 * Created by JinMiao
 * 2018/10/24.
 */
public class ScheduleTask implements ITask, Runnable, TimerTask {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleTask.class);

    protected final IMessageExecutor messageExecutor;

    protected final Ukcp ukcp;

    protected final Timer timer;

    public ScheduleTask(IMessageExecutor messageExecutor, Ukcp ukcp, Timer timer) {
        this.messageExecutor = messageExecutor;
        this.ukcp = ukcp;
        this.timer = timer;
    }

    /**
     * flush策略:
     * <ul>
     *     <li>在 <code>send</code> 调用后检查缓冲区如果可以发送直接调用update得到时间并存在ukcp内</li>
     *     <li>定时任务到了检查ukcp的时间和自己的定时 如果可以发送则直接发送  时间延后则重新定时 定时任务发送成功后检测缓冲区  是否触发发送时间</li>
     *     <li>读时间触发后检测检测缓冲区触发写事件</li>
     * </ul>
     * <p>
     * 问题: 精准大量的flush触发会导致ack重复发送   流量增大？  不会的 ack只会发送一次
     */
    @Override
    public void execute() {
        try {
            final Ukcp ukcp = this.ukcp;
            long now = System.currentTimeMillis();
            //判断连接是否关闭
            if (ukcp.getTimeoutMillis() != 0 && now - ukcp.getTimeoutMillis() > ukcp.getLastReceiveTime()) {
                ukcp.internalClose();
            }
            if (!ukcp.isActive()) {
                return;
            }
            long timeLeft = ukcp.getTsUpdate() - now;
            //判断执行时间是否到了
            if (timeLeft > 0) {
                timer.newTimeout(this, timeLeft, TimeUnit.MILLISECONDS);
                return;
            }
            long next = ukcp.flush(now);
            timer.newTimeout(this, next, TimeUnit.MILLISECONDS);
            //检测写缓冲区 如果能写则触发写事件
            if (!ukcp.getWriteBuffer().isEmpty() && ukcp.canSend(false)) {
                ukcp.notifyWriteEvent();
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void run() {
        this.messageExecutor.execute(this);
    }

    @Override
    public void run(Timeout timeout) {
        run();
    }
}
