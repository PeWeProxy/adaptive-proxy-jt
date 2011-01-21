package sk.fiit.peweproxy.plugins.services.impl.platform;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import sk.fiit.peweproxy.AdaptiveEngine;
import sk.fiit.peweproxy.messages.HttpMessageImpl;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.ProxyPlugin;
import sk.fiit.peweproxy.plugins.PluginHandler.PluginInstance;
import sk.fiit.peweproxy.plugins.services.impl.BaseServiceProvider;
import sk.fiit.peweproxy.services.platform.PlatformContextService;

public class PlatformContextImpl extends BaseServiceProvider<PlatformContextService>
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
			return adaptiveEngine.getIntegrationManager().isPluginEnabled(httpMessage, plgInstance.getName(), typeClass); 
		}
		
		@Override
		public void setEnabled(boolean enabled) {
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
	final String userId;
	
	public PlatformContextImpl(HttpMessageImpl<?> httpMessage, AdaptiveEngine adaptiveEngine) {
		super(httpMessage);
		this.adaptiveEngine = adaptiveEngine;
		this.userId = httpMessage.userIdentification();
	}


	@Override
	public List<PluginStatus> getPluginsStatuses(Class<? extends ProxyPlugin> pluginClass) {
		List<PluginStatus> retVal = createdInstances.get(pluginClass);
		if (retVal == null) {
			retVal = new LinkedList<PluginStatus>();
			String plgType = pluginClass.getSimpleName();
			for (PluginInstance plgInstance : adaptiveEngine.getPluginHandler().getAllPlugins()) {
				if (plgInstance.getTypes().contains(plgType))
					retVal.add(new PluginStatusImpl(plgInstance, pluginClass));
			}
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
		throw new RuntimeException("This service is @messageIdependent, no changes commiting should be called");
	}

	@Override
	public void doChanges(ModifiableHttpRequest request) {
		throw new RuntimeException("This service is @messageIdependent, no changes commiting should be called");
	}


	@Override
	protected Class<PlatformContextService> getServiceClass() {
		return PlatformContextService.class;
	}
}
