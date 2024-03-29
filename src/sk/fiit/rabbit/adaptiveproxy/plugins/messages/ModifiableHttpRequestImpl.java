package sk.fiit.rabbit.adaptiveproxy.plugins.messages;

import java.net.InetSocketAddress;

import sk.fiit.rabbit.adaptiveproxy.plugins.headers.HeaderWrapper;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.RequestServiceHandleImpl;

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
	public HeaderWrapper getProxyRequestHeaders() {
		return proxyRQHeaders;
	}

	@Override
	public HeaderWrapper getClientRequestHeaders() {
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
