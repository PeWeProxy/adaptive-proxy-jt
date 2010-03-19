package sk.fiit.rabbit.adaptiveproxy.messages;

import java.net.InetSocketAddress;

import sk.fiit.rabbit.adaptiveproxy.headers.HeaderWrapper;
import sk.fiit.rabbit.adaptiveproxy.services.ResponseServiceHandleImpl;

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
	public HeaderWrapper getProxyResponseHeader() {
		return proxyRPHeaders;
	}
	
	@Override
	public HeaderWrapper getWebResponseHeader() {
		return webRPHeaders;
	}
	
	@Override
	public ResponseServiceHandleImpl getServiceHandle() {
		return serviceHandle;
	}

	@Override
	public HeaderWrapper getProxyRequestHeader() {
		return request.getProxyRequestHeader();
	}

	@Override
	public HeaderWrapper getClientRequestHeader() {
		return request.getClientRequestHeader();
	}
	
	@Override
	public InetSocketAddress getClientSocketAddress() {
		return request.getClientSocketAddress();
	}
}
