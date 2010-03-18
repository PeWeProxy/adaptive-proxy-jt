package sk.fiit.rabbit.adaptiveproxy.plugins.services;

import sk.fiit.rabbit.adaptiveproxy.services.ProxyService;

public interface ServiceProvider {
	ProxyService getService();
	
	Class<? extends ProxyService> getServiceClass();
	
	void doChanges();
}
