package sk.fiit.rabbit.adaptiveproxy.plugins;

import java.util.Set;

import sk.fiit.rabbit.adaptiveproxy.headers.ResponseHeaders;
import sk.fiit.rabbit.adaptiveproxy.services.ProxyService;

public interface ResponsePlugin extends ProxyPlugin {
	Set<Class<? extends ProxyService>> desiredResponseServices(ResponseHeaders webRPHeaders);
}
