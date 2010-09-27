package rabbit.httpio.request;

import rabbit.httpio.BlockListener;
import rabbit.io.BufferHandle;
import rabbit.proxy.Connection;
import rabbit.proxy.TrafficLoggerHandler;
import sk.fiit.peweproxy.utils.InMemBytesStore;

public class ContentFetcher implements BlockListener {
	private final ContentCachingListener listener;
	private final DirectContentSource directSource;
	private final InMemBytesStore memStore;
	
	public ContentFetcher(Connection con, BufferHandle bufHandle, 
			TrafficLoggerHandler tlh, ContentSeparator separator,
			ContentCachingListener listener, long dataSize) {
		this.listener = listener;
		if (dataSize < 0)
			dataSize = 0;
		if (dataSize > Integer.MAX_VALUE)
			dataSize = Integer.MAX_VALUE;
		memStore = new InMemBytesStore((int)dataSize);
		directSource = new DirectContentSource(con,bufHandle,tlh,separator);
		directSource.readFirstBytes(this);
	}
	
	@Override
	public void bufferRead(BufferHandle bufHandle) {
		memStore.writeBuffer(bufHandle.getBuffer());
		directSource.readNextBytes();
	}
	
	@Override
	public void finishedRead() {
		listener.dataCached(memStore.getBytes(),memStore.getIncrements());
	}
	
	@Override
	public void failed(Exception e) {
		listener.failed(e);
	}

	@Override
	public void timeout() {
		// TODO do we need to try 5 times ?
		listener.timeout();
	}
}
