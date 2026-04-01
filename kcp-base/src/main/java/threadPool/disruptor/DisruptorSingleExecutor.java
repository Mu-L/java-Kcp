package threadPool.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import threadPool.IMessageExecutor;
import threadPool.ITask;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 {@link #disruptor} 的单线程队列实现
 *
 * @author King
 */
public class DisruptorSingleExecutor implements IMessageExecutor {
    /**
     * 主线程工厂
     **/
    private class LoopThreadfactory implements ThreadFactory {
        IMessageExecutor iMessageExecutor;

        public LoopThreadfactory(IMessageExecutor iMessageExecutor) {
            this.iMessageExecutor = iMessageExecutor;
        }

        @Override
        public Thread newThread(Runnable r) {
            DisruptorThread currentThread = new DisruptorThread(r, iMessageExecutor);
            currentThread.setName(threadName);
            return currentThread;
        }
    }

    private static final DisruptorEventHandler HANDLER = new DisruptorEventHandler();

    /**
     * RingBuffer长度：65536条消息
     */
    private static final int DEFAULT_RING_BUFFER_SIZE = 2 << 15;
    private final DisruptorEventFactory eventFactory = new DisruptorEventFactory();
    private final AtomicBoolean istop = new AtomicBoolean();

    /**
     * 线程名字
     */
    private final String threadName;

    private Disruptor<DisruptorHandler> disruptor = null;
    private RingBuffer<DisruptorHandler> buffer = null;

    public DisruptorSingleExecutor(String threadName) {
        this.threadName = threadName;
    }

    public void start() {
        LoopThreadfactory loopThreadfactory = new LoopThreadfactory(this);
//		disruptor = new Disruptor<DistriptorHandler>(eventFactory, ringBufferSize, executor, ProducerType.MULTI, strategy);
        disruptor = new Disruptor<>(eventFactory, DEFAULT_RING_BUFFER_SIZE, loopThreadfactory);
        buffer = disruptor.getRingBuffer();
        disruptor.handleEventsWith(DisruptorSingleExecutor.HANDLER);
        disruptor.start();
    }

    @Override
    public void stop() {
        if (istop.get()) {
            return;
        }
        disruptor.shutdown();

        istop.set(true);
    }

    public AtomicBoolean getIstop() {
        return istop;
    }

    @Override
    public boolean isFull() {
        return !buffer.hasAvailableCapacity(1);
    }

    @Override
    public void execute(ITask iTask) {
        //if(currentThread==this.currentThread){
        //	iTask.execute();
        //	return;
        //}
        //		if(buffer.hasAvailableCapacity(1))
//		{
//			System.out.println("没有容量了");
//		}
        long next = buffer.next();
        DisruptorHandler testEvent = buffer.get(next);
        testEvent.setTask(iTask);
        buffer.publish(next);
    }

    public static void main(String[] args) {
        DisruptorSingleExecutor disruptorSingleExecutor = new DisruptorSingleExecutor("aa");
        disruptorSingleExecutor.start();
        disruptorSingleExecutor.execute(() -> {
            System.out.println("hahaha");
        });

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
