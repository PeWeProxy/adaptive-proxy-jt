package rabbit.httpio.request;

import java.io.IOException;
import java.nio.ByteBuffer;
import rabbit.io.BufferHandle;
import rabbit.io.SimpleBufferHandle;
import org.khelekore.rnio.ReadHandler;
import rabbit.proxy.Connection;
import rabbit.proxy.TrafficLoggerHandler;

import rabbit.httpio.request.ContentChunksModifier.AsyncChunkDataModifiedListener;

public class DirectContentSource extends ContentSource {
	private final Connection con;
    private final BufferHandle bufHandle;
    private final TrafficLoggerHandler tlh;
    private final ContentSeparator separator;
    private boolean allDataPassed = false;
    private boolean chunkModifierNotified = false;
    private final ContentChunksModifier chunksModifier;
    
	public DirectContentSource(Connection con, BufferHandle bufHandle, TrafficLoggerHandler tlh,
			ContentSeparator separator, ContentChunksModifier chunksModifier) {
		this.con = con;
		this.bufHandle = bufHandle;
		this.tlh = tlh;
		this.separator = separator;
		this.chunksModifier = chunksModifier;
		if (separator.isAlreadyDone())
			allDataPassed = true;
	}

	@Override
	public void readNextBytes() {
		if (allDataPassed) {
			if (chunkModifierNotified)
				listener.finishedRead();
			else {
				chunkModifierNotified = true;
				if (chunksModifier != null) {
					// request chunk modifier does not initiate advance in request handling
					// (listener does), so we don't need to call him
					chunksModifier.finishedRead(new AsyncChunkDataModifiedListener() {
						@Override
						public void dataModified(byte[] newData) {
							if (newData != null && newData.length > 0) {
								listener.bufferRead(new SimpleBufferHandle(ByteBuffer.wrap(newData)));
							} else
								listener.finishedRead();
						}
					});
				} else
					listener.finishedRead();
			}
		} else if (!bufHandle.isEmpty())
			separateData();
		else
			waitForRead();
	}
	
	private void waitForRead () {
		bufHandle.possiblyFlush ();
		ReadHandler sh = new Reader ();
		con.getNioHandler ().waitForRead (con.getChannel (), sh);
	}
	
	private class Reader implements ReadHandler {
	private final Long timeout = con.getNioHandler ().getDefaultTimeout ();
	
	public void read () {
	    try {
		ByteBuffer buffer = bufHandle.getBuffer ();
		//buffer.limit (buffer.capacity ());
		int read = con.getChannel ().read (buffer);
		if (read == 0) {
		    waitForRead ();
		} else if (read == -1) {
		    failed (new IOException ("Failed to read request"));
		} else {
		    tlh.getClient ().read (read);
		    buffer.flip ();
		    separateData();
		}
	    } catch (IOException e) {
		listener.failed (e);
	    }
	}
	
	public void closed () {
	    bufHandle.possiblyFlush ();
	    listener.failed (new IOException ("Connection closed"));
	}
	
	public void timeout () {
	    bufHandle.possiblyFlush ();
	    listener.timeout ();
	}
	
	public boolean useSeparateThread () {
	    return false;
	}
	
	public String getDescription () {
	    return toString ();
	}
	
	public Long getTimeout () {
	    return timeout;
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
	    	if (buffer.hasRemaining())
	    	    buffer.compact();
	    	waitForRead();
		} else if (indicator == ContentSeparator.VAL_SEPARATED_UNFINISHED
				|| indicator == ContentSeparator.VAL_SEPARATED_FINISHED) {
			
			BufferHandle providedBuffer = bufHandle;
			if (buffer.limit() != limit) {
				// separation moved buffer's limit back, we need to pass only a slice
				providedBuffer = new SimpleBufferHandle(buffer.slice());
				buffer.position(buffer.limit());
				buffer.limit(limit);
			}
			allDataPassed = (indicator == ContentSeparator.VAL_SEPARATED_FINISHED);
			if (chunksModifier != null) {
				chunksModifier.modifyBuffer(providedBuffer, new ContentChunksModifier.AsyncChunkModifierListener() {
					@Override
					public void chunkModified(BufferHandle bufHandle) {
						listener.bufferRead(bufHandle);
					}
				});
			} else
				listener.bufferRead(providedBuffer);
		} else
			throw new IllegalStateException("Content separator returned unspecified value");
	}
	
	private void failed (Exception e) {
		bufHandle.possiblyFlush ();
		listener.failed(e);
	}
}
