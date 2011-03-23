package sk.fiit.peweproxy.services.platform;

import java.util.List;

import sk.fiit.peweproxy.plugins.ProxyPlugin;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ProxyService.readonly;

/**
 * Platform context service provides a mechanism to enable or disable integration of the
 * plugins to the process of handling of the messages of a particular user at different places
 * specified by proxy plugin type. It provides plugins with a way to find out which proxy plugins
 * are currently loaded and which of them are integrated as of particular plugin types into the 
 * process of handling messages of the user that initiated the message this service is provided
 * for. Also through this service plugins are able to toggle the integration of loaded plugins
 * for specific plugin types.
 * <br><br>
 * <b>Definition and implementation bundled</b><br>
 * <i>This service is one of the final services and service plugins are not allowed to
 * provide implementations of it. Definition of this service and also its implementation
 * is bundled with every release of the proxy server.</i>
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
@readonly
@SuppressWarnings("all") // to avoid compile time warning
public interface PlatformContextService extends ProxyService {
	
	/**
	 * Holds the information about the integration of particular plugin at places where the
	 * plugins of particular type are integrated into the processes of handling of HTTP messages
	 * of particular user in the AdaptiveProxy platform. Through plugin status instances, plugins
	 * are provided with the option to toggle integration of other plugins as being of specific
	 * type for specifric user.
	 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
	 */
	public interface PluginStatus {
		/**
		 * Get the runtime name of the plugin.
		 * @return name of the plugin
		 */
		public String getName();
		
		/**
		 * Get the actual runtime class of the plugin.
		 * @return actual class of the plugin
		 */
		public Class<? extends ProxyPlugin> getPluginClass();
		
		/**
		 * Get the type of the plugin which this instance holds status for. 
		 * @return type of the plugin
		 */
		public Class<? extends ProxyPlugin> getPluginType();
		
		/**
		 * Returns <code>true</code> when the plugin is used at places where the plugins of
		 * associated particular type are integrated into the AdaptiveProxy platform,
		 * <code>false</code> otherwise.
		 * @return whether the plugin is enabled for being used as particular type
		 */
		public boolean isEnabled();
		
		/**
		 * Sets whether the plugin will be uses at places where the plugins of associated
		 * particular type are integrated into the AdaptiveProxy platform.
		 * @param enabled whether the plugin should be enabled for being used as particular type
		 */
		public void setEnabled(boolean enabled);
	}
	
	/**
	 * Returns the list of plugin statuses for plugins loaded (enabled and disabled) by the
	 * AdaptiveProxy platform as being of type specified with <code>pluginClass</code>.
	 * @param pluginClass type of the plugins to get statuses for
	 * @return list of plugin statuses for all plugins loaded as passed type
	 */
	List<PluginStatus> getPluginsStatuses(Class<? extends ProxyPlugin> pluginClass);
	
	
	/**
	 * Convenience method to quickly get the status of specific plugin (specified by passed
	 * <code>plguinName</code>) loaded as being of specific type (passed <code>pluginClass</code>).
	 * @param plguinName name of the plugin to get plugin status instance for
	 * @param pluginClass type of the plugin to get status for
	 * @return plugin status instance for specified plugin loaded as passed type
	 */
	PluginStatus getPluginStatus(String plguinName, Class<? extends ProxyPlugin> pluginClass);
}
