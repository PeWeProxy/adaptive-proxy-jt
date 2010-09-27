package rabbit.httpio.request;

import java.nio.ByteBuffer;
import java.util.Queue;
import rabbit.io.SimpleBufferHandle;
import sk.fiit.peweproxy.utils.BytesChunker;

public class PrefetchedContentSource extends ContentSource {
	private final ByteBuffer contentByffer; 
	private final Queue<Integer> dataIncrements;
	
	public PrefetchedContentSource(byte[] contentData, Queue<Integer> dataIncrements) {
		this.dataIncrements = dataIncrements;
		contentByffer = ByteBuffer.wrap(contentData);
		dataIncrements = BytesChunker.adjustBytesIncrements(dataIncrements, contentData.length);
	}
	
	@Override
	void readNextBytes() {
		if (contentByffer.hasRemaining()) {
			int nextDataSize = dataIncrements.remove().intValue();
			contentByffer.limit(contentByffer.position()+nextDataSize);
			ByteBuffer buffer = contentByffer.slice();
			contentByffer.position(contentByffer.limit());
			contentByffer.limit(contentByffer.capacity());
			listener.bufferRead(new SimpleBufferHandle(buffer));
		} else
			listener.finishedRead();
	}
}
