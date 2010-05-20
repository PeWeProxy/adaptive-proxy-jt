package sk.fiit.rabbit.adaptiveproxy.messages;

import java.util.Arrays;

import sk.fiit.rabbit.adaptiveproxy.headers.HeaderWrapper;
import sk.fiit.rabbit.adaptiveproxy.services.ResponseServiceHandleImpl;
import sk.fiit.rabbit.adaptiveproxy.services.ModulesManager;

public final class ModifiableHttpResponseImpl extends HttpMessageImpl<ResponseServiceHandleImpl>
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
	public ModifiableHttpResponseImpl clone() {
		ModifiableHttpResponseImpl retVal = new ModifiableHttpResponseImpl(getServiceHandle().getManager()
				, webRPHeaders, proxyRPHeaders.clone(),  request);
		if (data != null)
			retVal.data = Arrays.copyOf(data, data.length);
		retVal.disableThreadCheck();
		return retVal;
	}
}
