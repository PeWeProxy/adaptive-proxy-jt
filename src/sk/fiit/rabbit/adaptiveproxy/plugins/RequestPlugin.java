package sk.fiit.rabbit.adaptiveproxy.plugins;

import java.util.Set;

import sk.fiit.rabbit.adaptiveproxy.plugins.headers.RequestHeaders;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;

public interface RequestPlugin extends ProxyPlugin {
	Set<Class<? extends ProxyService>> desiredRequestServices(RequestHeaders clientRQHeaders);
}
