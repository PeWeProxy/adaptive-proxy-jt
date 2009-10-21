package sk.fiit.rabbit.adaptiveproxy.plugins;

import sk.fiit.rabbit.adaptiveproxy.plugins.headers.ResponseHeaders;

public interface ResponsePlugin extends ProxyPlugin {
	boolean wantResponseContent(ResponseHeaders webRPHeaders);
}
