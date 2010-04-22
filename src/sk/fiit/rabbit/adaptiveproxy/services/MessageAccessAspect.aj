package sk.fiit.rabbit.adaptiveproxy.services;

import java.util.HashMap;
import java.util.Map;

import sk.fiit.rabbit.adaptiveproxy.AdaptiveEngine;
import sk.fiit.rabbit.adaptiveproxy.messages.HttpMessageImpl;
import sk.fiit.rabbit.adaptiveproxy.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.messages.HttpResponse;
import sk.fiit.rabbit.adaptiveproxy.headers.ReadableHeader;
import sk.fiit.rabbit.adaptiveproxy.headers.RequestHeader;
import sk.fiit.rabbit.adaptiveproxy.headers.ResponseHeader;
import sk.fiit.rabbit.adaptiveproxy.headers.WritableHeader;
import sk.fiit.rabbit.adaptiveproxy.headers.WritableRequestHeader;
import sk.fiit.rabbit.adaptiveproxy.headers.WritableResponseHeader;

public aspect MessageAccessAspect {
	private final Map<WritableHeader, ServicesHandleBase> handlesForHeaders;
	
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
