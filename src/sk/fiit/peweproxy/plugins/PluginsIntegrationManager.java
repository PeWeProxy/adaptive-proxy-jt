package sk.fiit.peweproxy.plugins;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.peweproxy.messages.HttpMessageImpl;
import sk.fiit.peweproxy.services.ServiceUnavailableException;
import sk.fiit.peweproxy.services.platform.PluginsTogglingService;

public class PluginsIntegrationManager {
	static final Logger log = Logger.getLogger(PluginsIntegrationManager.class);
	private final Map<String, UsersBlacklists> blacklistsForUsers = new HashMap<String, UsersBlacklists>();
	
	private class UsersBlacklists {
		final Map<Class<? extends ProxyPlugin>, Set<String>> blacklistsForType =
			new HashMap<Class<? extends ProxyPlugin>, Set<String>>();
		
		Set<String> getBlackList(Class<? extends ProxyPlugin> pluginType) {
			Set<String> retVal = blacklistsForType.get(pluginType);
			if (retVal == null) {
				retVal = new HashSet<String>();
				blacklistsForType.put(pluginType, retVal);
			}
			return retVal;
		}
	}
	
	private boolean togglingEnabled;
	
	public void setToggling(boolean enabled) {
		togglingEnabled = enabled;
	}
	
	public boolean isPluginEnabled(HttpMessageImpl<?> message, String pluginName, Class<? extends ProxyPlugin> pluginType) {
		if (!togglingEnabled)
			return true;
		return !getBlackList(message,pluginType).contains(pluginName);
	}
	
	public Set<String> getBlackList(HttpMessageImpl<?> message, Class<? extends ProxyPlugin> pluginType) {
		if (!togglingEnabled)
			return Collections.emptySet();
		String userId = message.userIdentification();
		if (userId == null)
			return Collections.emptySet();
		UsersBlacklists blackLists = blacklistsForUsers.get(userId);
		if (blackLists == null) {
			blackLists = new UsersBlacklists();
			blacklistsForUsers.put(userId, blackLists);
		}
		if (blackLists.blacklistsForType.get(pluginType) == null) {
			PluginsTogglingService togglingSvc = null;
			try {
				togglingSvc = message.getServicesHandleInternal().getService(PluginsTogglingService.class);
			} catch (ServiceUnavailableException ignored) {}
			while (togglingSvc != null) {
				try {
					blackLists.getBlackList(pluginType).addAll(togglingSvc.getPluginsBlacklist(pluginType));
				} catch (ServiceUnavailableException e) {
					log.warn(PluginsTogglingService.class.getSimpleName()+" realization threw exception" +
							"on calling getPluginsBlacklist() right after being provided", e);
				}
				// get blacklist from all service implementations
				try {
					togglingSvc = message.getServicesHandleInternal().getNextService(togglingSvc);
				} catch (ServiceUnavailableException ignored) {
					// no more implementations available
					togglingSvc = null;
				}
			}
		}
		return blackLists.getBlackList(pluginType);
	}
	
	
	public void setPluginEnabled(HttpMessageImpl<?> message, String pluginName, Class<? extends ProxyPlugin> pluginType, boolean enabled) {
		if (!togglingEnabled || message.userIdentification() == null)
			return;
		if (pluginType == null)
			throw new IllegalArgumentException("Plugin type can not be null");
		Set<String> blacklist = getBlackList(message, pluginType);
		if (enabled)
			blacklist.remove(pluginName);
		else
			blacklist.add(pluginName);
	}
}
