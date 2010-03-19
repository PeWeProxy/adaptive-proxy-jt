package sk.fiit.rabbit.adaptiveproxy.messages;

import java.net.InetSocketAddress;

import sk.fiit.rabbit.adaptiveproxy.headers.HeaderWrapper;
import sk.fiit.rabbit.adaptiveproxy.services.RequestServiceHandleImpl;

public final class ModifiableHttpRequestImpl extends HttpMessageImpl implements ModifiableHttpRequest {
	private InetSocketAddress clientSocketAdr;
	private HeaderWrapper clientRQHeaders;
	private HeaderWrapper proxyRQHeaders;
	private RequestServiceHandleImpl serviceHandle = null;
	
	public ModifiableHttpRequestImpl(HeaderWrapper clientRQHeaders, InetSocketAddress clientSocketAdr) {
		this.clientSocketAdr = clientSocketAdr;
		this.clientRQHeaders = clientRQHeaders;
		this.proxyRQHeaders = new HeaderWrapper(clientRQHeaders.getBackedHeader().clone());
		serviceHandle = new RequestServiceHandleImpl(this);
	}
	
	@Override
	public HeaderWrapper getProxyRequestHeader() {
		return proxyRQHeaders;
	}

	@Override
	public HeaderWrapper getClientRequestHeader() {
		return clientRQHeaders;
	}

	@Override
	public RequestServiceHandleImpl getServiceHandle() {
		return serviceHandle;
	}
	
	@Override
	public InetSocketAddress getClientSocketAddress() {
		return clientSocketAdr;
	}
}
