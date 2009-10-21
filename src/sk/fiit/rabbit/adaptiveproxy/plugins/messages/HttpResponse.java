package sk.fiit.rabbit.adaptiveproxy.plugins.messages;

import sk.fiit.rabbit.adaptiveproxy.plugins.headers.ResponseHeaders;

public interface HttpResponse extends HttpRequest {
		ResponseHeaders getWebResponseHeaders();
		
		ResponseHeaders getProxyResponseHeaders();
}
