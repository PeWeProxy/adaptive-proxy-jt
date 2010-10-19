package sk.fiit.peweproxy.messages;

import sk.fiit.peweproxy.headers.HeaderWrapper;
import sk.fiit.peweproxy.services.ModulesManager;
import sk.fiit.peweproxy.services.ResponseServiceHandleImpl;

public final class ModifiableHttpResponseImpl extends HttpMessageImpl<ResponseServiceHandleImpl, ModifiableHttpResponse>
		implements ModifiableHttpResponse {
	private final ModifiableHttpRequestImpl request;
	private final HeaderWrapper webRPHeaders;
	private final HeaderWrapper proxyRPHeaders;
	
	public ModifiableHttpResponseImpl(ModulesManager modulesManager, HeaderWrapper webRPHeaders, ModifiableHttpRequestImpl request) {
		// webRPHeaders are those that are going to be modified by RabbIT code
		this(modulesManager, webRPHeaders.clone(), webRPHeaders, request);
	}
	
	private ModifiableHttpResponseImpl(ModulesManager modulesManager, HeaderWrapper webRPHeaders, HeaderWrapper proxyRPHeaders, ModifiableHttpRequestImpl request) {
		this.request = request;
		this.webRPHeaders = webRPHeaders;
		this.proxyRPHeaders = proxyRPHeaders;
		webRPHeaders.setHttpMessage(this);
		proxyRPHeaders.setHttpMessage(this);
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
	public HeaderWrapper getOriginalHeader() {
		return webRPHeaders;
	}

	@Override
	public HeaderWrapper getProxyHeader() {
		return proxyRPHeaders;
	}
	
	@Override
	public void setAllowedThread() {
		super.setAllowedThread();
		request.setAllowedThread();
	}
	
	@Override
	protected ModifiableHttpResponseImpl makeClone() {
		return new ModifiableHttpResponseImpl(getServicesHandle().getManager()
				, webRPHeaders, proxyRPHeaders.clone(), (ModifiableHttpRequestImpl)request.clone());
	}
}
