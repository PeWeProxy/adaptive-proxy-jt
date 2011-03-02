package sk.fiit.peweproxy.messages;

import java.util.Arrays;

import org.apache.log4j.Logger;

import sk.fiit.peweproxy.headers.HeaderWrapper;
import sk.fiit.peweproxy.services.ChunksRemainsImpl;
import sk.fiit.peweproxy.services.ServicesHandle;
import sk.fiit.peweproxy.services.ServicesHandleBase;
import sk.fiit.peweproxy.utils.InMemBytesStore;
import sk.fiit.peweproxy.utils.StackTraceUtils;

public abstract class HttpMessageImpl<HandleType extends ServicesHandle> implements HttpMessage {
	protected static final Logger log = Logger.getLogger(HttpMessageImpl.class);
	private static final String VOID_USERID = HttpMessageImpl.class.getSimpleName()+"_VOID_USERID";
	
	protected final HeaderWrapper header;
	private InMemBytesStore dataStore = null;
	byte[] data = null;
	private HandleType serviceHandle;
	private boolean checkThread = false;
	private Thread allowedThread;
	private boolean isReadonly = false;
	private boolean isComlpete = false;
	private boolean chunkProcessed = false;
	private ChunksRemainsImpl chunkRemains = new ChunksRemainsImpl();
	protected String userId = VOID_USERID;
	
	public HttpMessageImpl(HeaderWrapper header) {
		this.header = header;
		header.setHttpMessage(this);
	}
	
	protected void setServicesHandle(HandleType serviceHandle) {
		this.serviceHandle = serviceHandle;
		if (log.isDebugEnabled())
			log.debug(toString()+" uses "+serviceHandle.toString());
	}
	
	public void addData(byte[] data) {
		if (data == null || data.length == 0)
			return;
		if (dataStore == null) {
			int size = data.length;
			if (this.data != null)
				size += this.data.length;
			dataStore = new InMemBytesStore(size);
			if (this.data != null)
				dataStore.writeArray(this.data, 0, this.data.length);
		}
		dataStore.writeArray(data, 0, data.length);
		this.data = null;
	}
	
	public void setData(byte[] data) {
		this.data = data;
		this.dataStore = null;
		isComlpete = true;
	}
	
	public byte[] getData() {
		if (data == null && dataStore != null)
			data = dataStore.getBytes();
		return data;
	}
	
	public HandleType getServicesHandleInternal() {
		return serviceHandle;
	}
	
	@Override
	public HandleType getServicesHandle() {
		return serviceHandle;
	}
	
	@Override
	public boolean bodyAccessible() {
		return data != null || dataStore != null;
	}
	
	public void setComlpete() {
		if (dataStore != null) {
			data = dataStore.getBytes();
			dataStore = null;
		}
		isComlpete = true;
	}
	
	@Override
	public boolean isComplete() {
		return isComlpete;
	}
	
	@Override
	public boolean wasChunkProcessed() {
		return chunkProcessed;
	}
	
	public void setChunkProcessed() {
		this.chunkProcessed = true;
	}
	
	@Override
	public String getUserIdentification() {
		return userIdentification();
	}
	
	public String userIdentification() {
		if (userId == VOID_USERID)
			userId = ((ServicesHandleBase<?>)serviceHandle).getUserId();
		return userId;
	}
	
	public HeaderWrapper getHeader() {
		return header;
	}
	
	public abstract HttpMessageImpl<HandleType> originalMessage();
	
	public void setAllowedThread() {
		checkThread = true;
		Thread lastThread = allowedThread;
		allowedThread = Thread.currentThread();
		if (lastThread != allowedThread && log.isTraceEnabled())
			log.trace(this+" changing allowed thread to "+allowedThread);
	}
	
	public void disableThreadCheck() {
		checkThread = false;
	}
	
	public void checkThreadAccess() {
		if (checkThread) {
			Thread curThread = Thread.currentThread();
			if (log.isTraceEnabled())
				log.trace(this+" checking executing thread "+curThread+" against allowed thread "+allowedThread);
			if (curThread != allowedThread) {
				StackTraceElement[] trace = curThread.getStackTrace();
				boolean internalThread = false;
				for (StackTraceElement stackTraceElement : trace) {
					if (stackTraceElement.getClassName().startsWith("org.khelekore.rnio")) {
						internalThread = true;
						break;
					}
				}
				if (internalThread) {
					log.warn("Internal thread was not recognized as allowed, fix this !\n"+StackTraceUtils.getStackTraceText(curThread));
					return;
				}
	            UnsupportedOperationException e =  new UnsupportedOperationException("Access to the message is not allowed" +
						" from this thread");
	            log.info("Attempt to access a message from other thread",e);
	            throw e;
			}
		}
	}
	
	public void setReadOnly() {
		isReadonly = true;
	}
	
	public boolean isReadOnly() {
		return isReadonly;
	}
	
	public ChunksRemainsImpl getChunkRemains() {
		return chunkRemains;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
	}
	
	public HttpMessageImpl<HandleType> clone() {
		// to avoid compile error, this method is never called
		return null;
	}
	
	protected <MessageType extends HttpMessageImpl<HandleType>> MessageType clone(MessageType message) {
		if (data != null)
			message.data = Arrays.copyOf(data, data.length);
		else if (dataStore != null)
			message.data = dataStore.getBytes();
		message.disableThreadCheck();
		message.isReadonly = isReadonly;
		message.userId = userId;
		message.isComlpete = isComlpete;
		message.chunkProcessed = chunkProcessed;
		return message;
	}
}
