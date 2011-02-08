package rabbit.httpio.request;

import rabbit.httpio.AsyncListener;

public interface ContentCachingListener extends AsyncListener{
	void dataCached(byte[] contentData);
}
