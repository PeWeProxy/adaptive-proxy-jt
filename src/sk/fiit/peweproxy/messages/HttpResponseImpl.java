package sk.fiit.peweproxy.messages;

import sk.fiit.peweproxy.headers.HeaderWrapper;
import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.services.ModulesManager;
import sk.fiit.peweproxy.services.ResponseServiceHandleImpl;
import sk.fiit.peweproxy.utils.StackTraceUtils;

public class HttpResponseImpl extends HttpMessageImpl<ResponseServiceHandleImpl>
	implements HttpResponse {
	
	protected final HttpRequestImpl request;

	public HttpResponseImpl(ModulesManager modulesManager, HeaderWrapper header, HttpRequestImpl request) {
		super(header);
		if (request == null) // temporary assert-like check 
			throw new NullPointerException("Constructing response with request set to null\n"+StackTraceUtils.getStackTraceText());
		this.request = request;
		this.userId = request.userId;
		setServicesHandle(new ResponseServiceHandleImpl(this,modulesManager));
	}
	
	@Override
	public HttpRequest getRequest() {
		return request;
	}
	
	@Override
	public HttpResponse getOriginalResponse() {
		return originalMessage();
	}
	
	@Override
	public HttpResponseImpl originalMessage() {
		// this class is for representations of original responses, let them
		// point to self when asked for original message
		return this;
	}

	@Override
	public ResponseHeader getResponseHeader() {
		return header;
	}
	
	@Override
	public void setAllowedThread() {
		super.setAllowedThread();
		request.setAllowedThread();
	}

	@Override
	public HttpResponseImpl clone() {
		return clone(new HttpResponseImpl(getServicesHandle().getManager(),
				header.clone(), request));
	}
}
