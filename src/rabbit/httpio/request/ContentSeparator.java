package rabbit.httpio.request;

import rabbit.io.BufferHandle;

public interface ContentSeparator {
	public static final byte VAL_UNSPECIFIED = -1;
	public static final byte VAL_SEPARATED_UNFINISHED = 1;
	public static final byte VAL_SEPARATED_FINISHED = 2;
	public static final byte VAL_SEPARATED_NEEDMOREDATA = 3;
	
	byte separateData(BufferHandle bufHandle) throws Exception;
}