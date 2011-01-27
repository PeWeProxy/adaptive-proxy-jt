package sk.fiit.peweproxy.messages;

import sk.fiit.peweproxy.headers.HeaderWrapper;
import sk.fiit.peweproxy.headers.WritableResponseHeader;
import sk.fiit.peweproxy.services.ModulesManager;

public final class ModifiableHttpResponseImpl extends HttpResponseImpl
		implements ModifiableHttpResponse {
	private final HttpResponseImpl originalResponse;
	
	public ModifiableHttpResponseImpl(ModulesManager modulesManager, HeaderWrapper header,
			HttpResponseImpl originalResponse) {
		super(modulesManager, header, originalResponse.request);
		this.originalResponse = originalResponse;
		log.trace(toString()+" has original response set to "+originalResponse.toString());
	}
	
	public ModifiableHttpResponseImpl(ModulesManager modulesManager, HeaderWrapper header,
			HttpRequestImpl request) {
		super(modulesManager, header, request);
		//this response constructed in request processing is going to be referenced as original response
		this.originalResponse = this;
		log.trace(toString()+" has original response set to self");
	}
	
	@Override
	public WritableResponseHeader getResponseHeader() {
		return header;
	}
	
	@Override
	public HttpResponse getOriginalResponse() {
		return originalMessage();
	}
	
	public HttpResponseImpl originalMessage() {
		return originalResponse;
	}
	
	@Override
	public void setAllowedThread() {
		super.setAllowedThread();
		if (originalResponse != this)	//if acting as original response
			originalResponse.setAllowedThread();
	}
	
	@Override
	public ModifiableHttpResponseImpl clone() {
		return clone(new ModifiableHttpResponseImpl(getServicesHandle().getManager(),
				header, originalResponse));
	}
}
