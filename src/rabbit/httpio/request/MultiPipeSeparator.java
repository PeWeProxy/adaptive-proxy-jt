package rabbit.httpio.request;

import rabbit.io.BufferHandle;
import rabbit.proxy.MultiPartPipe;

public class MultiPipeSeparator implements ContentSeparator {
	private final MultiPartPipe mpp ;
	
	public MultiPipeSeparator(String contentTypeHeader) {
		mpp = new MultiPartPipe(contentTypeHeader);
	}
	
	@Override
	public boolean isAlreadyDone() {
		return false;
	}
	
	@Override
	public byte separateData(BufferHandle bufHandle) {
		mpp.parseBuffer(bufHandle.getBuffer());
		return (mpp.isFinished()) ? VAL_SEPARATED_FINISHED : VAL_SEPARATED_UNFINISHED;
	}
}
