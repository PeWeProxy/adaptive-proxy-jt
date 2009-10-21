package sk.fiit.rabbit.adaptiveproxy.plugins.helpers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.headers.RequestHeaders;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpRequest;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.RequestServicePlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.RequestServiceProvider;

public class RequestServicePluginAdapter implements RequestServicePlugin {

	@Override
	public List<RequestServiceProvider> provideRequestServices(HttpRequest request) {
		List<RequestServiceProvider> providedServices = new ArrayList<RequestServiceProvider>(1);
		addProvidedRequestServices(providedServices);
		return providedServices;
	}

	protected void addProvidedRequestServices(List<RequestServiceProvider> providedServices) {
	}

	@Override
	public Set<Class<? extends ProxyService>> getDependencies() {
		Set<Class<? extends ProxyService>> deps = new HashSet<Class<? extends ProxyService>>();
		addDependencies(deps);
		return deps;
	}

	protected void addDependencies(Set<Class<? extends ProxyService>> deps) {
	}

	@Override
	public Set<Class<? extends ProxyService>> getProvidedServices() {
		Set<Class<? extends ProxyService>> providedServices = new HashSet<Class<? extends ProxyService>>();
		addProvidedServices(providedServices);
		return providedServices;

	}

	protected void addProvidedServices(
			Set<Class<? extends ProxyService>> providedServices) {
	}

	@Override
	public boolean setup(PluginProperties props) {
		return true;
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public boolean supportsReconfigure() {
		return true;
	}

	@Override
	public boolean wantRequestContent(RequestHeaders clientRQHeaders) {
		return false;
	}

}
