package sk.fiit.peweproxy.utils;

import java.util.LinkedList;
import java.util.Queue;

public class BytesChunker {
	private static final int DEF_BUF_SIZE = 4096;
	
	// TODO ma cenu upravovat aby sa to podobalo tomu co bolo prijmane ?
	// (kedze to nemuselo byt a asi ani neblo tak odosielane - NIO/Buffers/reuse)
	public static Queue<Integer> adjustBytesIncrements(Queue<Integer> increments, int dataSize) {
		Queue<Integer> retVal = new LinkedList<Integer>();
		while (dataSize > 0) {
			int chunk = (int) Math.min(dataSize, DEF_BUF_SIZE);
			retVal.add(new Integer(chunk));
			dataSize -= chunk;
		}
		return retVal;
	}
}
