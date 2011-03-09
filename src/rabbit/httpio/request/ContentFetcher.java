package rabbit.httpio.request;

import java.nio.ByteBuffer;

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
		ByteBuffer buf = bufHandle.getBuffer();
		// if buf is bufHandle that DirectContentSource uses for reading, we have to
		// exhaust the buffer so that DirectContentSource won'try to separate it's content 
		buf.position(buf.limit());
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
