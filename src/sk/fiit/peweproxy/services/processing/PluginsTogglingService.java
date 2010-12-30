package sk.fiit.peweproxy.services.processing;

import java.util.Set;

import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.user.UserIdentificationService;

/**
 * Processing plugins toggling service provides a mechanism to enable or disable the processing
 * of the messages of a particular user by a particular processing plugin. Using this service,
 * other plugins can toggle whether the AdaptiveProxy platform will call particular request /
 * response processing plugin to process future request / response messages initiated by the
 * user that initiated the message this service is provided for.<br><br>
 * This service depends on the presence of the realization of the
 * {@link UserIdentificationService} service during message processing. When there's no module
 * that provide user identification service plugged in, or none of such modules is able to
 * identify the user that initiated the message, this service won't be available.
 * <br><br>
 * <b>Definition and implementation bundled</b><br>
 * <i>This service is one of the five final services and service plugins are not
 * allowed to provide implementations of it. Definition of this service and also
 * its implementation is bundled with every release of the proxy server.</i>
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface PluginsTogglingService extends ProxyService {
	
	public interface PluginStatus {
		/**
		 * Returns the name of the processing plugin this status object holds integration flag
		 * for.
		 * @return name of the processing plugin
		 */
		public String getPluginName();
		
		/**
		 * Returns <code>true</code> if the plugin is called to process the messages of the
		 * particular user, <code>false</code> if it's skipped during processing of the
		 * messages.
		 * @return whether the plugin is integrated into messages processing
		 */
		public boolean isEnabled();
		
		/**
		 * Returns <code>true</code> if the processing plugin, this status object holds
		 * integration flag for, is currently loaded and running, <code>false</code> otherwise. 
		 * @return whether is the processing plugin currently running
		 */
		public boolean isRunning();
	}
	
	@readonly
	public Set<PluginStatus> getRequestPluginsStatus();
	
	@readonly
	public Set<PluginStatus> getResponsePluginsStatus();
	
	@readonly
	public void setRequestPluginStatus(String pluginName, boolean newStatus);
	
	@readonly
	public void setResponsePluginStatus(String pluginName, boolean newStatus);
}
