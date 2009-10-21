package rabbit.httpio.request;

import java.io.IOException;
import rabbit.io.BufferHandle;
import rabbit.proxy.MultiPartPipe;

public class MultiPipeSeparator implements ContentSeparator {
	private final MultiPartPipe mpp ;
	
	public MultiPipeSeparator(String contentTypeHeader) {
		mpp = new MultiPartPipe(contentTypeHeader);
	}
	
	@Override
	public byte separateData(BufferHandle bufHandle) {
		try {
			mpp.parseBuffer(bufHandle.getBuffer());
		} catch (IOException ignored) {}
		return (mpp.isFinished()) ? VAL_SEPARATED_FINISHED : VAL_SEPARATED_UNFINISHED;
	}
}
