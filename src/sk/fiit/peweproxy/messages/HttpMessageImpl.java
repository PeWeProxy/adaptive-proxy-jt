package sk.fiit.peweproxy.messages;

import java.util.Arrays;

import org.apache.log4j.Logger;

import sk.fiit.peweproxy.headers.HeaderWrapper;
import sk.fiit.peweproxy.services.ServicesHandle;
import sk.fiit.peweproxy.services.ServicesHandleBase;
import sk.fiit.peweproxy.utils.StackTraceUtils;

public abstract class HttpMessageImpl<HandleType extends ServicesHandle> implements HttpMessage {
	protected static final Logger log = Logger.getLogger(HttpMessageImpl.class);
	private static final String VOID_USERID = HttpMessageImpl.class.getSimpleName()+"_VOID_USERID";
	
	protected final HeaderWrapper header;
	byte[] data = null;
	private HandleType serviceHandle;
	private boolean checkThread = false;
	private Thread allowedThread;
	private boolean isReadonly = false;
	protected String userId;
	
	public HttpMessageImpl(HeaderWrapper header) {
		this.header = header;
		header.setHttpMessage(this);
	}
	
	protected void setServicesHandle(HandleType serviceHandle) {
		this.serviceHandle = serviceHandle;
		if (log.isDebugEnabled())
			log.debug(toString()+" uses "+serviceHandle.toString());
	}
	
	public void setData(byte[] data) {
		this.data = data;
	}
	
	public byte[] getData() {
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
	public boolean hasBody() {
		return data != null;
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
		message.disableThreadCheck();
		message.isReadonly = isReadonly;
		message.userId = userId;
		return message;
	}
}
