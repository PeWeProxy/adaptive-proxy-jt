package sk.fiit.peweproxy.messages;

import sk.fiit.peweproxy.headers.HeaderWrapper;
import sk.fiit.peweproxy.headers.WritableRequestHeader;
import sk.fiit.peweproxy.services.ModulesManager;

public final class ModifiableHttpRequestImpl extends HttpRequestImpl
		implements ModifiableHttpRequest {
	private final HttpRequestImpl originalRequest;
	
	public ModifiableHttpRequestImpl(ModulesManager modulesManager, HeaderWrapper header,
			HttpRequestImpl originalRequest) {
		super(modulesManager,header, originalRequest.clientSocketAddress());
		this.originalRequest = originalRequest;
		this.userId = originalRequest.userId;
		if (log.isTraceEnabled())
			log.trace(toString()+" has original request set to "+originalRequest.toString());
		
	}
	
	@Override
	public WritableRequestHeader getRequestHeader() {
		return header;
	}

	@Override
	public HttpRequest getOriginalRequest() {
		return originalMessage();
	}
	
	public HttpRequestImpl originalMessage() {
		return originalRequest;
	}
	
	@Override
	public void setAllowedThread() {
		super.setAllowedThread();
		originalRequest.setAllowedThread();
	}
	
	
	@Override
	public ModifiableHttpRequestImpl clone() {
		return clone(new ModifiableHttpRequestImpl(getServicesHandle().getManager(),
				header.clone(), originalRequest));
	}
}
