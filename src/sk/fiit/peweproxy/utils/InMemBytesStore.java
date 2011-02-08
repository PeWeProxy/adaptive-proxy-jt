package sk.fiit.peweproxy.utils;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class InMemBytesStore {
	public static final int DEF_ARRAYSIZE = 4096;
	
	private final ByteArrayOutputStream stream;
	
	public InMemBytesStore(long initSize) {
		if (initSize < 1) {
			initSize = DEF_ARRAYSIZE;
		}
		stream = new ByteArrayOutputStream((int)initSize);
	}
	
	public void writeBuffer(ByteBuffer buffer) {
		writeBuffer(buffer,true);
	}
	
	private void writeBuffer(ByteBuffer buffer, boolean advancePos) {
		int size = buffer.remaining();
		byte[] array = null;
		int offset = buffer.position();
		if (buffer.hasArray()) {
			array = buffer.array();
			if (advancePos)
				buffer.position(buffer.limit());
		} else {
			array = new byte[size];
			buffer.get(array);
			if (!advancePos)
				buffer.position(offset);
			offset = 0;
		}
		writeArray(array,offset,size);
	}
	
	public void writeBufferKeepPosition(ByteBuffer buffer) {
		writeBuffer(buffer,false);
	}
	
	public void writeArray(byte[] bytes, int offset, int len) {
		stream.write(bytes,offset,len);
	}
	
	public byte[] getBytes() {
		return stream.toByteArray();
	}
	
	public int getSize() {
		return stream.size();
	}
	
	public static ByteBuffer chunkBufferForSend(ByteBuffer buffer) {
		int toRead = buffer.remaining();
		if (toRead > 0) {
			if (DEF_ARRAYSIZE < toRead) {
				toRead = DEF_ARRAYSIZE;
				int limit = buffer.limit();
				buffer.limit(buffer.position()+toRead);
				ByteBuffer retVal = buffer.slice();
				buffer.position(buffer.limit());
				buffer.limit(limit);
				return retVal;
			} else
				return buffer;
		} else
			return null;
	}
}
