package sk.fiit.rabbit.adaptiveproxy.plugins;

import sk.fiit.rabbit.adaptiveproxy.plugins.headers.RequestHeaders;

public interface RequestPlugin extends ProxyPlugin {
	boolean wantRequestContent(RequestHeaders clientRQHeaders);
}
