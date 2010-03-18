package sk.fiit.rabbit.adaptiveproxy.messages;

import sk.fiit.rabbit.adaptiveproxy.headers.ResponseHeaders;

public interface HttpResponse extends HttpRequest {
		ResponseHeaders getWebResponseHeaders();
		
		ResponseHeaders getProxyResponseHeaders();
}
