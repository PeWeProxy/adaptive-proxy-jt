package sk.fiit.rabbit.adaptiveproxy.messages;

import sk.fiit.rabbit.adaptiveproxy.headers.WritableRequestHeaders;

public interface ModifiableHttpRequest extends HttpRequest {
	WritableRequestHeaders getProxyRequestHeaders();
}
