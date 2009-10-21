package rabbit.httpio.request;

import java.nio.ByteBuffer;

import rabbit.io.BufferHandle;

public class FixedLengthSeparator implements ContentSeparator {
	private final long dataSize;
	private long dataPassed = 0;
	
	public FixedLengthSeparator(long dataSize) {
		this.dataSize = dataSize;
	}

	@Override
	public byte separateData(BufferHandle bufHandle) {
		ByteBuffer buffer = bufHandle.getBuffer();
		long toTransfer = Math.min (buffer.remaining (), dataSize - dataPassed);
		buffer.limit(buffer.position()+(int)toTransfer);
		dataPassed += toTransfer;
		return (dataPassed < dataSize) ? VAL_SEPARATED_UNFINISHED : VAL_SEPARATED_FINISHED;
	}

}
