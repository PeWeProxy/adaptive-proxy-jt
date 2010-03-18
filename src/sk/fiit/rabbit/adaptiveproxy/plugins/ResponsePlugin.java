package sk.fiit.rabbit.adaptiveproxy.plugins;

import java.util.Set;

import sk.fiit.rabbit.adaptiveproxy.plugins.headers.ResponseHeaders;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;

public interface ResponsePlugin extends ProxyPlugin {
	Set<Class<? extends ProxyService>> desiredResponseServices(ResponseHeaders webRPHeaders);
}
