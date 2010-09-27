package sk.fiit.peweproxy.services;

import java.util.HashMap;
import java.util.Map;

import sk.fiit.peweproxy.AdaptiveEngine;
import sk.fiit.peweproxy.headers.ReadableHeader;
import sk.fiit.peweproxy.headers.RequestHeader;
import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.headers.WritableHeader;
import sk.fiit.peweproxy.headers.WritableRequestHeader;
import sk.fiit.peweproxy.headers.WritableResponseHeader;
import sk.fiit.peweproxy.messages.HttpMessageImpl;
import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.messages.HttpResponse;

public aspect MessageAccessAspect {
	@SuppressWarnings("unchecked")
	private final Map<WritableHeader, ServicesHandleBase> handlesForHeaders;
	
	@SuppressWarnings("unchecked")
	public MessageAccessAspect() {
		handlesForHeaders = new HashMap<WritableHeader, ServicesHandleBase>();
	}
	
	@SuppressWarnings("unchecked")
	after(ServicesHandleBase svcHandle,	HttpMessageImpl<?> httpMessage)
		: initialization(ServicesHandleBase.new(..)) && this(svcHandle) && args(httpMessage,..) {
		WritableHeader header = httpMessage.getProxyHeader();
		//String headerString = header.getClass().getSimpleName()+"@"+Integer.toHexString(header.hashCode());
		handlesForHeaders.put(header, svcHandle);
	}
	
	pointcut readableHeaderMethods() : execution(* (RequestHeader || ResponseHeader).*(..));
	pointcut writableHeaderMethods() : execution(* (WritableRequestHeader|| WritableResponseHeader) .*(..));
	pointcut inRequestProcessing() : cflow(execution(* AdaptiveEngine.runRequestAdapters(..)));
	pointcut inResponseProcessing() : cflow(execution(* AdaptiveEngine.runResponseAdapters(..)));
	pointcut inMessageProcessing() : inRequestProcessing() || inResponseProcessing();
	@SuppressWarnings("unchecked")
	before(ReadableHeader header) : readableHeaderMethods() && this(header)
			&& !cflowbelow(readableHeaderMethods()) && inMessageProcessing() {
		ServicesHandleBase svcHandle = handlesForHeaders.get(header);
		if (svcHandle != null) {
			svcHandle.httpMessage.checkThreadAccess();
			svcHandle.headerBeingRead();
		}
	}
	@SuppressWarnings("unchecked")
	before(WritableHeader header) : writableHeaderMethods() && this(header)
			&& !cflowbelow(writableHeaderMethods()) && !readableHeaderMethods() 
			&& inMessageProcessing() {
		ServicesHandleBase svcHandle = handlesForHeaders.get(header);
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
