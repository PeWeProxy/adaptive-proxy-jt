package sk.fiit.peweproxy.messages;

import org.apache.log4j.Logger;

import sk.fiit.peweproxy.headers.HeaderWrapper;
import sk.fiit.peweproxy.services.ServicesHandle;

public abstract class HttpMessageImpl<HandleType extends ServicesHandle> implements HttpMessage {
	private final static Logger log = Logger.getLogger(HttpMessageImpl.class);
		
	byte[] data = null;
	private HandleType serviceHandle;
	private boolean checkThread = false;
	private Thread allowedThread;
	
	protected void setServiceHandle(HandleType serviceHandle) {
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
	
	@Override
	public HandleType getServicesHandle() {
		return serviceHandle;
	}
	
	@Override
	public boolean hasBody() {
		return data != null;
	}
	
	public abstract HeaderWrapper getOriginalHeader();
	
	public abstract HeaderWrapper getProxyHeader();
	
	public void setAllowedThread() {
		checkThread = true;
		allowedThread = Thread.currentThread();
	}
	
	public void disableThreadCheck() {
		checkThread = false;
	}
	
	public void checkThreadAccess() {
		if (checkThread) {
			Thread curThread = Thread.currentThread();
			if (curThread != allowedThread) {
				StackTraceElement[] trace = curThread.getStackTrace();
				boolean internalThread = false;
				for (StackTraceElement stackTraceElement : trace) {
					if (stackTraceElement.getClassName().startsWith("rabbit.nio")) {
						internalThread = true;
						break;
					}
				}
				if (internalThread) {
					StringBuilder sb = new StringBuilder();
		            for (int i=1; i < trace.length; i++) {
		                sb.append("\tat ");
		                sb.append(trace[i]);
		                sb.append('\n');
		            }
					log.warn("Internal thread was not recognized as allowed, fix this !\nStackTrace:\n"+sb.toString());
					return;
				}
	            UnsupportedOperationException e =  new UnsupportedOperationException("Access to the message is not allowed" +
						" from this thread");
	            log.info("Attempt to access a message from other thread",e);
	            throw e;
			}
		}
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
	}
	
	@Override
	public abstract HttpMessage clone();
}
