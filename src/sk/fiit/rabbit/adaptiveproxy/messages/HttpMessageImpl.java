package sk.fiit.rabbit.adaptiveproxy.messages;

import sk.fiit.rabbit.adaptiveproxy.headers.ReadableHeader;
import sk.fiit.rabbit.adaptiveproxy.headers.WritableHeader;
import sk.fiit.rabbit.adaptiveproxy.services.ServicesHandle;

public abstract class HttpMessageImpl<HandleType extends ServicesHandle> implements HttpMessage {
	byte[] data = null;
	private HandleType serviceHandle;
	
	protected void setServiceHandle(HandleType serviceHandle) {
		this.serviceHandle = serviceHandle;
	}
	
	public void setData(byte[] data) {
		this.data = data;
	}
	
	public byte[] getData() {
		return data;
	}
	
	@Override
	public HandleType getServiceHandle() {
		return serviceHandle;
	}
	
	@Override
	public boolean hasBody() {
		return data != null;
	}
	
	public abstract ReadableHeader getOriginalHeader();
	
	public abstract WritableHeader getProxyHeader();
}
