package sk.fiit.rabbit.adaptiveproxy.plugins.helpers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.headers.ResponseHeaders;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ProxyService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ResponseServicePlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ResponseServiceProvider;

public class ResponseServicePluginAdapter implements ResponseServicePlugin {

	@Override
	public List<ResponseServiceProvider> provideResponseServices(
			HttpResponse response) {
		List<ResponseServiceProvider> providedServices = new ArrayList<ResponseServiceProvider>(1);
		addProvidedResponseServices(providedServices);
		return providedServices;
	}

	protected void addProvidedResponseServices(
			List<ResponseServiceProvider> providedServices) {
	}

	@Override
	public Set<Class<? extends ProxyService>> getDependencies() {
		Set<Class<? extends ProxyService>> dependencies = new HashSet<Class<? extends ProxyService>>();
		addDependencies(dependencies);
		return dependencies;
	}

	protected void addDependencies(Set<Class<? extends ProxyService>> dependencies) {
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
	public boolean wantResponseContent(ResponseHeaders webRPHeaders) {
		return false;
	}

}
