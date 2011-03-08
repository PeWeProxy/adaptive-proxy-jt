package rabbit.httpio.request;

import java.nio.ByteBuffer;
import java.util.Arrays;

import rabbit.io.BufferHandle;
import rabbit.io.SimpleBufferHandle;

public abstract class ContentChunksModifier {
	
	public interface AsyncChunkModifierListener {
		public void chunkModified(BufferHandle bufHandle);
	}
	
	public interface AsyncChunkDataModifiedListener {
		public void dataModified(byte[] newData);
	}
	
	/**
	 * Meni position <i>bufferHandle</i> a moze vratit inu BufferHandle !
	 * @param bufferHandle
	 * @return
	 */
	public void modifyBuffer(final BufferHandle bufHandle, final AsyncChunkModifierListener modifiedListener) {
		final ByteBuffer buffer = bufHandle.getBuffer();
		byte[] data = new byte[buffer.remaining()];
		final int pos = buffer.position();
		buffer.get(data);
		modifyData(data, new AsyncChunkDataModifiedListener() {
			@Override
			public void dataModified(byte[] data) {
				BufferHandle bufferHandle = bufHandle;
				if (data != null && data.length > 0) {
					// if data is null or empty, this is not executed and
					// the buffer is left with no data remaining
					buffer.position(pos);
					
					if (data.length <= buffer.remaining()) {
						buffer.put(data);
						buffer.limit(buffer.position());
						buffer.position(pos);
					} else {
						bufferHandle = new SimpleBufferHandle(ByteBuffer.wrap(data));
						buffer.position(buffer.limit());
					}
				}
				modifiedListener.chunkModified(bufferHandle);
			}
		});
	}
	
	public void modifyArray(byte[] bytes, int offset, int len, final AsyncChunkDataModifiedListener modifiedListener) {
		byte[] data = Arrays.copyOfRange(bytes, offset, len);
		modifyData(data, modifiedListener);
	}
	
	/**
	 * This method is executed in NIO thread !
	 * @param data
	 * @return
	 */
	abstract protected void modifyData(byte[] data, AsyncChunkDataModifiedListener listener);
	
	abstract public void finishedRead(AsyncChunkDataModifiedListener listener);
}
