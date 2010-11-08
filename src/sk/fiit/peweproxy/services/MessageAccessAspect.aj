package sk.fiit.peweproxy.services;


import sk.fiit.peweproxy.AdaptiveEngine;
import sk.fiit.peweproxy.headers.HeaderWrapper;
import sk.fiit.peweproxy.headers.RequestHeader;
import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.headers.WritableRequestHeader;
import sk.fiit.peweproxy.headers.WritableResponseHeader;
import sk.fiit.peweproxy.messages.HttpMessageImpl;
import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.messages.HttpResponse;

public aspect MessageAccessAspect {
	/*@SuppressWarnings("unchecked")
	private final Map<ReadableHeader, ServicesHandleBase<?,?>> handlesForHeaders;
	
	@SuppressWarnings("unchecked")
	public MessageAccessAspect() {
		handlesForHeaders = new HashMap<ReadableHeader, ServicesHandleBase<?,?>>();
	}
	
	@SuppressWarnings("unchecked")
	after(ServicesHandleBase svcHandle,	HttpMessageImpl<?,?> httpMessage)
		: initialization(ServicesHandleBase.new(..)) && this(svcHandle) && args(httpMessage,..) {
		handlesForHeaders.put(httpMessage.getProxyHeader(), svcHandle);
		handlesForHeaders.put(httpMessage.getOriginalHeader(), svcHandle);
	}
	
	public void removeReferences(ServicesHandleBase<?,?> svcHandle) {
		// if there's another thread with reference to this message, we should not delete references
		// need referencing message from header
		handlesForHeaders.remove(svcHandle.httpMessage.getProxyHeader());
		handlesForHeaders.remove(svcHandle.httpMessage.getOriginalHeader());
	}
	*/
	
	pointcut readableHeaderMethods() : execution(* (RequestHeader || ResponseHeader).*(..));
	pointcut writableHeaderMethods() : execution(* (WritableRequestHeader|| WritableResponseHeader) .*(..));
	pointcut inRequestProcessing() : cflow(execution(* AdaptiveEngine.runRequestAdapters(..)));
	pointcut inResponseProcessing() : cflow(execution(* AdaptiveEngine.runResponseAdapters(..)));
	pointcut inMessageProcessing() : inRequestProcessing() || inResponseProcessing();
	@SuppressWarnings("unchecked")
	before(HeaderWrapper header) : readableHeaderMethods() && this(header)
			&& !cflowbelow(readableHeaderMethods()) && inMessageProcessing() {
		ServicesHandleBase<?> svcHandle = (ServicesHandleBase<?>) header.getHttpMessage().getServicesHandle();
		if (svcHandle != null) {
			svcHandle.httpMessage.checkThreadAccess();
			svcHandle.headerBeingRead();
		}
	}
	@SuppressWarnings("unchecked")
	before(HeaderWrapper header) : writableHeaderMethods() && this(header)
			&& !cflowbelow(writableHeaderMethods()) && !readableHeaderMethods() 
			&& inMessageProcessing() {
		ServicesHandleBase<?> svcHandle = (ServicesHandleBase<?>) header.getHttpMessage().getServicesHandle();
		if (svcHandle != null) {
			svcHandle.httpMessage.checkThreadAccess();
			svcHandle.headerBeingModified();
		}
	}
	
	@SuppressWarnings("unchecked")
	pointcut servicesHandleMethod(ServicesHandleBase svcHandle)
		: execution(* ServicesHandle.*(..)) && this(svcHandle);
	
	@SuppressWarnings("unchecked")
	pointcut messageMethod(HttpMessageImpl message)
		: execution(* (HttpRequest || HttpResponse).*(..)) && this(message);
	
	@SuppressWarnings("unchecked")
	before(ServicesHandleBase svcHandle) : servicesHandleMethod(svcHandle) {
		svcHandle.httpMessage.checkThreadAccess();
	}
	@SuppressWarnings("unchecked")
	before(HttpMessageImpl message) : messageMethod(message) {
		message.checkThreadAccess();
	}
}
