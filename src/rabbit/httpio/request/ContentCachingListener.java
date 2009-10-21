package rabbit.httpio.request;

import java.util.Queue;

public interface ContentCachingListener {
	void dataCached(byte[] contentData, Queue<Integer> dataIncrements);
	
	void timeout();
	
	void failed(Exception e);
}
