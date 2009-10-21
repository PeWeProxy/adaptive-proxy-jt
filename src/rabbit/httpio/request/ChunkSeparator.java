package rabbit.httpio.request;

import rabbit.httpio.BlockListener;
import rabbit.httpio.ChunkDataFeeder;
import rabbit.httpio.ChunkHandler;
import rabbit.io.BufferHandle;

public class ChunkSeparator implements ContentSeparator, ChunkDataFeeder {
	private final ChunkHandler chunkHandler;
	
	private BufferHandle returnedBufferHandle = null;
	private Exception exception = null;
	private byte state = VAL_UNSPECIFIED;
	
	public ChunkSeparator (boolean strictHTTP) {
		chunkHandler = new ChunkHandler(this, strictHTTP);
		chunkHandler.addBlockListener(new ChunkBlockListener());
	}
	
	@Override
	public byte separateData(BufferHandle bufHandle) throws Exception {
		chunkHandler.handleData(bufHandle);
		if (state == VAL_UNSPECIFIED) {
			throw exception;
		}
		byte retVal = state;
		state = VAL_UNSPECIFIED;
		if (returnedBufferHandle != null && returnedBufferHandle != bufHandle) {
			bufHandle.getBuffer().limit(returnedBufferHandle.getBuffer().limit());
		}
		returnedBufferHandle = null;
		return retVal;
	}
	
	@Override
	public void finishedRead() {
		//empty, not called by ChunkHandler
	}

	@Override
	public void readMore() {
		state = VAL_SEPARATED_NEEDMOREDATA;
	}

	@Override
	public void register() {
		//empty, not called by ChunkHandler
	}
	
	private class ChunkBlockListener implements BlockListener {
		@Override
		public void bufferRead(BufferHandle bufHandle) {
			returnedBufferHandle = bufHandle;
			state = VAL_SEPARATED_UNFINISHED;
		}

		@Override
		public void finishedRead() {
			state = VAL_SEPARATED_FINISHED;
		}

		@Override
		public void failed(Exception cause) {
			exception = cause;
			state = VAL_UNSPECIFIED;
		}

		@Override
		public void timeout() {
			//empty, not called by ChunkHandler
		}
	}

}
