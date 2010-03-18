package sk.fiit.rabbit.adaptiveproxy.plugins.services;

import java.util.Set;
import sk.fiit.rabbit.adaptiveproxy.plugins.ProxyPlugin;
import sk.fiit.rabbit.adaptiveproxy.services.ProxyService;

public interface ServicePlugin extends ProxyPlugin {
	Set<Class<? extends ProxyService>> getDependencies();
	
	Set<Class<? extends ProxyService>> getProvidedServices();
}
