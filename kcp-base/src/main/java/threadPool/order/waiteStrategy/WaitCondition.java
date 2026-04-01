package threadPool.order.waiteStrategy;

/**
 * 等待条件
 * @param <T>
 */
public interface WaitCondition<T> {
	
	/**
	 * @return 附件
	 */
	T getAttach();
	
}
