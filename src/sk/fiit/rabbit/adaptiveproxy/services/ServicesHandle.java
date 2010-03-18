package sk.fiit.rabbit.adaptiveproxy.services;

import java.util.List;
import java.util.Set;


public interface ServicesHandle {
	// TODO do Javadocu ze instancia vzdy obsahuje StringContentService
	// a pocas volania pluginov aj ModifiableContentService, ak ma sprava
	// nejaky obsah (content)
	public Set<Class<? extends ProxyService>> getAvailableServices();
	
	<T extends ProxyService> T getService(Class<T> serviceClass) throws ServiceUnavailableException;
	
	<T extends ProxyService> List<T> getServices(Class<T> serviceClass) throws ServiceUnavailableException;
}
