package sk.fiit.rabbit.adaptiveproxy.utils;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

public class InMemBytesStore {
	private static final int DEF_ARRAYSIZE = 4096;
	
	private final ByteArrayOutputStream stream;
	private final Queue<Integer> dataIncrements;
	
	public InMemBytesStore(int initSize) {
		if (initSize < 0) {
			throw new IllegalArgumentException("initSize can not be negative");
		}
		if (initSize == 0) {
			initSize = DEF_ARRAYSIZE;
		}
		stream = new ByteArrayOutputStream((int)initSize);
		dataIncrements = new LinkedList<Integer>();
	}
	
	public void writeBuffer(ByteBuffer buffer) {
		int size = buffer.remaining();
		byte[] array = null;
		int offset = buffer.position();
		if (buffer.hasArray()) {
			array = buffer.array();
		} else {
			array = new byte[size];
			buffer.get(array);
			offset = 0;
		}
		writeArray(array,offset,size);
		buffer.position(buffer.limit());
	}
	
	public void writeArray(byte[] bytes, int offset, int len) {
		stream.write(bytes,offset,len);
		dataIncrements.add(new Integer(len));
	}
	
	public byte[] getBytes() {
		return stream.toByteArray();
	}
	
	public int getSize() {
		return stream.size();
	}
	
	public Queue<Integer> getIncrements() {
		Queue<Integer> retValue = new LinkedList<Integer>();
		for (Integer integer : dataIncrements) {
			retValue.add(integer);
		}
		return retValue;
	}
}
