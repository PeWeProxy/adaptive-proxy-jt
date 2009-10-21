package sk.fiit.rabbit.adaptiveproxy.plugins.services;

public interface ServiceProvider {
	ProxyService getService();
	
	Class<? extends ProxyService> getServiceClass();
	
	void doChanges();
}
