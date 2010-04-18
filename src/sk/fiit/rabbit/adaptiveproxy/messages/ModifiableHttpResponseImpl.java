package sk.fiit.rabbit.adaptiveproxy.messages;

import sk.fiit.rabbit.adaptiveproxy.headers.HeaderWrapper;
import sk.fiit.rabbit.adaptiveproxy.headers.ResponseHeader;
import sk.fiit.rabbit.adaptiveproxy.headers.WritableResponseHeader;
import sk.fiit.rabbit.adaptiveproxy.services.ResponseServiceHandleImpl;
import sk.fiit.rabbit.adaptiveproxy.services.ServiceModulesManager;

public final class ModifiableHttpResponseImpl extends HttpMessageImpl<ResponseServiceHandleImpl>
		implements ModifiableHttpResponse {
	private final ModifiableHttpRequestImpl request;
	private final HeaderWrapper webRPHeaders;
	private final HeaderWrapper proxyRPHeaders;
	
	public ModifiableHttpResponseImpl(ServiceModulesManager modulesManager, HeaderWrapper webRPHeaders, ModifiableHttpRequestImpl request) {
		this.request = request;
		this.webRPHeaders = new HeaderWrapper(webRPHeaders.getBackedHeader().clone());
		// webRPHeaders are those that are going to be modified by RabbIT code
		this.proxyRPHeaders = webRPHeaders;
		setServiceHandle(new ResponseServiceHandleImpl(this,modulesManager));
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
	public HttpRequest getRequest() {
		return request;
	}

	@Override
	public ResponseHeader getOriginalHeader() {
		return webRPHeaders;
	}

	@Override
	public WritableResponseHeader getProxyHeader() {
		return proxyRPHeaders;
	}
}
