package sk.fiit.rabbit.adaptiveproxy.plugins;

import java.util.Set;

import sk.fiit.rabbit.adaptiveproxy.headers.RequestHeaders;
import sk.fiit.rabbit.adaptiveproxy.services.ProxyService;

public interface RequestPlugin extends ProxyPlugin {
	Set<Class<? extends ProxyService>> desiredRequestServices(RequestHeaders clientRQHeaders);
}
