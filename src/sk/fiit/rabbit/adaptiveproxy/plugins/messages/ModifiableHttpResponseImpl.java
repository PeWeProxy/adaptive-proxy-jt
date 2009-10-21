package sk.fiit.rabbit.adaptiveproxy.plugins.messages;

import java.net.InetSocketAddress;

import sk.fiit.rabbit.adaptiveproxy.plugins.headers.HeaderWrapper;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ResponseServiceHandleImpl;

public final class ModifiableHttpResponseImpl extends HttpMessageImpl implements ModifiableHttpResponse {
	private final ModifiableHttpRequestImpl request;
	private final HeaderWrapper webRPHeaders;
	private final HeaderWrapper proxyRPHeaders;
	private final ResponseServiceHandleImpl serviceHandle;
	
	public ModifiableHttpResponseImpl(HeaderWrapper webRPHeaders, ModifiableHttpRequestImpl request) {
		this.request = request;
		this.webRPHeaders = new HeaderWrapper(webRPHeaders.getBackedHeader().clone());
		// webRPHeaders are those that are going to be modified by RabbIT code
		this.proxyRPHeaders = webRPHeaders;
		serviceHandle = new ResponseServiceHandleImpl(this);
	}
	
	@Override
	public HeaderWrapper getProxyResponseHeaders() {
		return proxyRPHeaders;
	}
	
	@Override
	public HeaderWrapper getWebResponseHeaders() {
		return webRPHeaders;
	}
	
	@Override
	public ResponseServiceHandleImpl getServiceHandle() {
		return serviceHandle;
	}

	@Override
	public HeaderWrapper getProxyRequestHeaders() {
		return request.getProxyRequestHeaders();
	}

	@Override
	public HeaderWrapper getClientRequestHeaders() {
		return request.getClientRequestHeaders();
	}
	
	@Override
	public InetSocketAddress getClientSocketAddress() {
		return request.getClientSocketAddress();
	}
}
