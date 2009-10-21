package sk.fiit.rabbit.adaptiveproxy.plugins.messages;

import sk.fiit.rabbit.adaptiveproxy.plugins.headers.WritableRequestHeaders;

public interface ModifiableHttpRequest extends HttpRequest {
	WritableRequestHeaders getProxyRequestHeaders();
}
