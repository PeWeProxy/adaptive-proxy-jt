package sk.fiit.peweproxy.plugins.services.impl.platform;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import sk.fiit.peweproxy.AdaptiveEngine;
import sk.fiit.peweproxy.messages.HttpMessageImpl;
import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.ProxyPlugin;
import sk.fiit.peweproxy.plugins.PluginHandler.PluginInstance;
import sk.fiit.peweproxy.plugins.services.impl.BaseServiceProvider;
import sk.fiit.peweproxy.services.DataHolder;
import sk.fiit.peweproxy.services.ServicesHandle;
import sk.fiit.peweproxy.services.platform.PlatformContextService;

public class PlatformContextImpl extends BaseServiceProvider<Object, PlatformContextService>
	implements PlatformContextService {
		
	private class PluginStatusImpl implements PluginStatus {
		final PluginInstance plgInstance;
		final Class<? extends ProxyPlugin> typeClass;
		
		public PluginStatusImpl(PluginInstance plgInstance, Class<? extends ProxyPlugin> typeClass) {
			this.plgInstance = plgInstance;
			this.typeClass = typeClass;
		}
		
		@Override
		public String getName() {
			return plgInstance.getName();
		}

		@Override
		public Class<? extends ProxyPlugin> getPluginClass() {
			return plgInstance.getPluginClass();
		}
		
		@Override
		public Class<? extends ProxyPlugin> getPluginType() {
			return typeClass;
		}

		@Override
		public boolean isEnabled() {
			if (userId == null)
				return true;
			return adaptiveEngine.getIntegrationManager().isPluginEnabled(httpMessage, plgInstance.getName(), typeClass); 
		}
		
		@Override
		public void setEnabled(boolean enabled) {
			if (userId != null)
				// change enabled status only when we can identify user
				adaptiveEngine.getIntegrationManager().setPluginEnabled(httpMessage, plgInstance.getName(), typeClass, enabled);
		}
		
		@Override
		public String toString() {
			boolean enabled = isEnabled();
			return getClass().getSimpleName()+"["+plgInstance.getName()+","+((enabled) ? "enabled" : "disabled")+"]";
		}
	}
	
	final AdaptiveEngine adaptiveEngine;
	final  Map<Class<? extends ProxyPlugin>, List<PluginStatus>> createdInstances = 
		new HashMap<Class<? extends ProxyPlugin>, List<PluginStatus>>();
	final HttpMessageImpl<?> httpMessage;
	final String userId;
	
	public PlatformContextImpl(HttpMessageImpl<?> httpMessage, AdaptiveEngine adaptiveEngine) {
		this.httpMessage = httpMessage;
		this.userId = httpMessage.userIdentification();
		this.adaptiveEngine = adaptiveEngine;
	}


	@Override
	public List<PluginStatus> getPluginsStatuses(Class<? extends ProxyPlugin> pluginClass) {
		List<PluginStatus> retVal = createdInstances.get(pluginClass);
		if (retVal == null) {
			retVal = new LinkedList<PluginStatus>();
			for (PluginInstance plgInstance : adaptiveEngine.getPluginHandler().getAllPlugins()) {
				if (plgInstance.getTypes().contains(pluginClass))
					retVal.add(new PluginStatusImpl(plgInstance, pluginClass));
			}
			createdInstances.put(pluginClass, retVal);
		}
		return retVal;
	}
	
	@Override
	public PluginStatus getPluginStatus(String plguinName, Class<? extends ProxyPlugin> pluginClass) {
		for (PluginStatus ldPlugin : getPluginsStatuses(pluginClass)) {
			if (ldPlugin.getName().equals(plguinName))
				return ldPlugin;
		}
		return null;
	}

	@Override
	public void doChanges(ModifiableHttpResponse response) {
		throw new RuntimeException("This service is @readonly, no changes commiting should be called");
	}

	@Override
	public void doChanges(ModifiableHttpRequest request) {
		throw new RuntimeException("This service is @readonly, no changes commiting should be called");
	}


	@Override
	protected Class<PlatformContextService> getServiceClass() {
		return PlatformContextService.class;
	}


	@Override
	public void doChanges(HttpRequest request, ServicesHandle chunkServicesHandle) {}


	@Override
	public void doChanges(HttpResponse response, ServicesHandle chunkServicesHandle) {}
	
	@Override
	public void ceaseContent(Object chunkPart, DataHolder dataHolder) {};
}
