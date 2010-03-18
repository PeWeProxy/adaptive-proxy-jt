package sk.fiit.rabbit.adaptiveproxy.plugins.services;

import java.util.List;

import sk.fiit.rabbit.adaptiveproxy.messages.HttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.ResponsePlugin;

/**
 * Interface for response service modules. Response service plugin is a service module
 * that provides implementations of services over particular HTTP responses.
 * <br><br>
 * <b>Proxy plugin interafce</b><br>
 * <i>This is an interface for one type of proxy plugins. Entities implementing this
 * interface are pluggable.</i>
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ResponseServiceModule extends ServiceModule, ResponsePlugin {
	/**
	 * Returns list of response service providers that provide implementation
	 * of particular services over passed response message.
	 * @param response response message to provide request service providers for
	 * @return list of request service implementation providers
	 */
	List<ResponseServiceProvider> provideResponseServices(HttpResponse response);
}
