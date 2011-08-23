package rabbit.httpio.request;

import java.nio.ByteBuffer;
import rabbit.io.SimpleBufferHandle;
import sk.fiit.peweproxy.utils.InMemBytesStore;

public class PrefetchedContentSource extends ContentSource {
	private final ByteBuffer contentBuffer; 
	
	public PrefetchedContentSource(byte[] contentData) {
		contentBuffer = ByteBuffer.wrap(contentData);
	}
	
	@Override
	void readNextBytes() {
		if (contentBuffer.hasRemaining()) {
			listener.bufferRead(new SimpleBufferHandle(InMemBytesStore.chunkBufferForSend(contentBuffer)));
		} else
			listener.finishedRead();
	}
}
