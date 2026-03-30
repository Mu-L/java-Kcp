package threadPool.disruptor;

import com.lmax.disruptor.EventHandler;

public class DisruptorEventHandler implements EventHandler<DisruptorHandler> {

    @Override
    public void onEvent(DisruptorHandler event, long sequence,
                        boolean endOfBatch) {
        event.execute();
    }
}
