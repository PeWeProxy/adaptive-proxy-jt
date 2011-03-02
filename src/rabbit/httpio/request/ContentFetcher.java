package rabbit.httpio.request;

import rabbit.httpio.BlockListener;
import rabbit.io.BufferHandle;
import rabbit.proxy.Connection;
import rabbit.proxy.TrafficLoggerHandler;

public class ContentFetcher implements BlockListener {
	private final Connection con;
	private final DirectContentSource directSource;
	
	public ContentFetcher(Connection con, BufferHandle bufHandle, 
			TrafficLoggerHandler tlh, ContentSeparator separator,
			ContentChunksModifier chunksModifier) {
		this.con = con;
		directSource = new DirectContentSource(con,bufHandle,tlh,separator,chunksModifier);
		directSource.readFirstBytes(this);
	}
	
	@Override
	public void bufferRead(BufferHandle bufHandle) {
		con.fireResouceDataRead (bufHandle);
		// no need to call listener.bufferRead(bufHandle) since DirectContentSource
		// is passing read chunks to chunksModifier
		directSource.readNextBytes();
	}
	
	@Override
	public void finishedRead() {
		// do nothing since chunkModifier already initiated advance in request handling
	}
	
	@Override
	public void failed(Exception e) {
		con.readFailed(e);
	}

	@Override
	public void timeout() {
		con.readTimeout();
	}
}
