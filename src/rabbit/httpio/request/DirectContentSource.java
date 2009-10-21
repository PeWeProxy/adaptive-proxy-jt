package rabbit.httpio.request;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import rabbit.io.BufferHandle;
import rabbit.io.SelectorRegistrator;
import rabbit.io.SimpleBufferHandle;
import rabbit.io.SocketHandler;
import rabbit.proxy.Connection;
import rabbit.proxy.TrafficLoggerHandler;

public class DirectContentSource extends ContentSource {
	private final Connection con;
    private final BufferHandle bufHandle;
    private final TrafficLoggerHandler tlh;
    private final ContentSeparator separator;
    private boolean allDataPassed = false;
    
	public DirectContentSource(Connection con, BufferHandle bufHandle, 
			TrafficLoggerHandler tlh, ContentSeparator separator) {
		this.con = con;
		this.bufHandle = bufHandle;
		this.tlh = tlh;
		this.separator = separator;
	}

	@Override
	public void readNextBytes() {
		if (allDataPassed)
			listener.finishedRead();
		else if (!bufHandle.isEmpty())
			separateData();
		else
			waitForRead();
	}
	
	private void waitForRead () {
		bufHandle.possiblyFlush ();
		SocketHandler sh = new Reader ();
		try {
		    SelectorRegistrator.register (con.getLogger (), 
						  con.getChannel (), 
						  con.getSelector (), 
						  SelectionKey.OP_READ, sh);
		} catch (IOException e) {
		    listener.failed (e);
		}
	}
	
	private void separateData() {
		ByteBuffer buffer = bufHandle.getBuffer();
		int limit = buffer.limit();
	    byte indicator = ContentSeparator.VAL_UNSPECIFIED;
		try {
			indicator = separator.separateData(bufHandle);
		} catch (Exception e) {
			failed(e);
			return;
		}
		if (indicator == ContentSeparator.VAL_SEPARATED_NEEDMOREDATA) {
	    	if (!bufHandle.isEmpty())
	    	    bufHandle.getBuffer().compact();
	    	waitForRead();
		} else if (indicator == ContentSeparator.VAL_SEPARATED_UNFINISHED
				|| indicator == ContentSeparator.VAL_SEPARATED_FINISHED) {
			BufferHandle providedBuffer = new SimpleBufferHandle(buffer.slice());
			buffer.position(buffer.limit());
			buffer.limit(limit);
			allDataPassed = (indicator == ContentSeparator.VAL_SEPARATED_FINISHED);
			listener.bufferRead(providedBuffer);
		} else
			throw new IllegalStateException("Content separator returned unspecified value");
	}

	private class Reader implements SocketHandler {
		public void run () {
			try {
				ByteBuffer buffer = bufHandle.getBuffer();
				buffer.limit (buffer.capacity());
				int read = con.getChannel().read(buffer);
				if (read == 0) {
				    waitForRead();
				} else if (read == -1) {
				    failed(new IOException ("Failed to read request"));
				} else {
				    tlh.getClient().read(read);
				    buffer.flip();
				    separateData();
				}
			} catch (IOException e) {
				listener.failed (e);
			}
		}

		public void timeout () {
			bufHandle.possiblyFlush ();
			listener.timeout();
		}

		public boolean useSeparateThread () {
			return false;
		}

		public String getDescription () {
		    return toString ();
		}
	}
	
	private void failed (Exception e) {
		bufHandle.possiblyFlush ();
		listener.failed(e);
	}
}
