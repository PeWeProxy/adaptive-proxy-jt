package sk.fiit.rabbit.adaptiveproxy.plugins.services;

import java.util.List;

import sk.fiit.rabbit.adaptiveproxy.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.RequestPlugin;

/**
 * Interface for request service modules. Request service module is a service module
 * that provides implementations of services over particular HTTP requests.
 * <br><br>
 * <b>Proxy plugin interafce</b><br>
 * <i>This is an interface for one type of proxy plugins. Entities implementing this
 * interface are pluggable.</i>
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface RequestServiceModule extends ServiceModule, RequestPlugin {
	/**
	 * Returns list of request service providers that provide implementation
	 * of particular services over passed request message.
	 * @param request request message to provide request service providers for
	 * @return list of request service implementation providers
	 */
	List<RequestServiceProvider> provideRequestServices(HttpRequest request);
}
