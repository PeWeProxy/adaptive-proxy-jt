package rabbit.httpio.request;

import rabbit.httpio.BlockListener;
import rabbit.io.BufferHandle;
import rabbit.proxy.Connection;
import rabbit.proxy.TrafficLoggerHandler;

public class ContentFetcher implements BlockListener {
	private final Connection con;
	private final ContentCachingListener listener;
	private final DirectContentSource directSource;
	
	public ContentFetcher(Connection con, BufferHandle bufHandle, 
			TrafficLoggerHandler tlh, ContentSeparator separator,
			ContentCachingListener listener) {
		this.con = con;
		this.listener = listener;
		directSource = new DirectContentSource(con,bufHandle,tlh,separator,listener);
		directSource.readFirstBytes(this);
	}
	
	@Override
	public void bufferRead(BufferHandle bufHandle) {
		con.fireResouceDataRead (bufHandle);
		directSource.readNextBytes();
	}
	
	@Override
	public void finishedRead() {
		// empty since DirectContentSource notifies listener with dataCached()
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
