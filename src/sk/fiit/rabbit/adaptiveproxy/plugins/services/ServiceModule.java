package sk.fiit.rabbit.adaptiveproxy.plugins.services;

import java.util.Set;
import sk.fiit.rabbit.adaptiveproxy.plugins.ProxyPlugin;
import sk.fiit.rabbit.adaptiveproxy.services.ProxyService;

/**
 * Base interface for all service modules. Service module is a proxy plugin
 * that provides implementations of services over particular HTTP messages.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ServiceModule extends ProxyPlugin {
	/**
	 * Returns dependencies of this service module. Dependencies of this service module
	 * is a set of classes of other services, that this module uses to realize services
	 * it provides implementation for. 
	 * @return set of service classes this service module depends on
	 */
	Set<Class<? extends ProxyService>> getDependencies();
	
	/**
	 * Returns set of service classes which this service module is able to provide
	 * implementation for, depending on particular messages context. 
	 * @return set of service classes this service module provides implementation for
	 */
	Set<Class<? extends ProxyService>> getProvidedServices();
}
