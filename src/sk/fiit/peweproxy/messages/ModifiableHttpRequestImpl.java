package sk.fiit.peweproxy.messages;

import sk.fiit.peweproxy.headers.HeaderWrapper;
import sk.fiit.peweproxy.headers.WritableRequestHeader;
import sk.fiit.peweproxy.services.ModulesManager;

public final class ModifiableHttpRequestImpl extends HttpRequestImpl
		implements ModifiableHttpRequest {
	private final HttpRequestImpl originalRequest;
	
	public ModifiableHttpRequestImpl(ModulesManager modulesManager, HeaderWrapper header,
			HttpRequestImpl originalRequest) {
		super(modulesManager, header, originalRequest.clientSocketAddress());
		this.originalRequest = originalRequest;
	}
	
	@Override
	public WritableRequestHeader getRequestHeader() {
		return header;
	}

	@Override
	public HttpRequest getOriginalRequest() {
		return originalRequest;
	}
	
	public HttpRequest originalRequest() {
		return originalRequest;
	}
	
	@Override
	public ModifiableHttpRequestImpl clone() {
		return clone(new ModifiableHttpRequestImpl(getServicesHandle().getManager(),
				header.clone(), originalRequest));
	}
	
	@Override
	public String toString() {
		return super.toString()+"["+originalRequest.toString()+"]";
	}
}
