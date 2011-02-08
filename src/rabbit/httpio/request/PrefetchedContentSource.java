package rabbit.httpio.request;

import java.nio.ByteBuffer;
import rabbit.io.SimpleBufferHandle;
import sk.fiit.peweproxy.utils.InMemBytesStore;

public class PrefetchedContentSource extends ContentSource {
	private final ByteBuffer contentByffer; 
	
	public PrefetchedContentSource(byte[] contentData) {
		contentByffer = ByteBuffer.wrap(contentData);
	}
	
	@Override
	void readNextBytes() {
		if (contentByffer.hasRemaining()) {
			listener.bufferRead(new SimpleBufferHandle(InMemBytesStore.chunkBufferForSend(contentByffer)));
		} else
			listener.finishedRead();
	}
}
