package sk.fiit.peweproxy.services.platform;

import java.util.List;

import sk.fiit.peweproxy.plugins.ProxyPlugin;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ProxyService.messageIdependent;

/**
 * Platform context service provides plugins a way to find out which proxy plugins are
 * currently loaded and enabled for the user that initiated the message this service is
 * provided for.
 * <br><br>
 * <b>Definition and implementation bundled</b><br>
 * <i>This service is one of the final services and service plugins are not allowed to
 * provide implementations of it. Definition of this service and also its implementation
 * is bundled with every release of the proxy server.</i>
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
@messageIdependent
public interface PlatformContextService extends ProxyService {
	
	public interface PluginStatus {
		public String getName();
		
		public Class<? extends ProxyPlugin> getPluginClass();
		
		public Class<? extends ProxyPlugin> getPluginType();
		
		public boolean isEnabled();
		
		public void setEnabled(boolean enabled);
		
		//TODO javadoc: toString
	}
	
	List<PluginStatus> getPluginsStatuses(Class<? extends ProxyPlugin> pluginClass);
	
	
	//TODO javadoc: convenience
	PluginStatus getPluginStatus(String plguinName, Class<? extends ProxyPlugin> pluginClass);
}
