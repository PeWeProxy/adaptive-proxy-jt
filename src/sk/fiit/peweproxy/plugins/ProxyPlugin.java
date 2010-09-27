package sk.fiit.peweproxy.plugins;

/**
 * Base interface for all proxy plugins. This interface defines methods for configuring
 * / reconfiguring, starting and stopping proxy plugins that all types of proxy plugins
 * are required to provide.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ProxyPlugin {
	/**
	 * Returns whether this proxy plugin may be reconfigured in it's current state with
	 * new configuration properties <code>newProps</code>.
	 * @return <code>true</code> if this plugin may be reconfigured in it's current state
	 * with passed properties, <code>false</code> otherwise.
	 */
	boolean supportsReconfigure(PluginProperties newProps);
	
	/**
	 * This method is called to configure (reconfigure) and start this proxy plugin. In this
	 * method the plugin should allocate all needed resources and do all possibly time
	 * consuming preparations according to passed properties <code>props</code> loaded
	 * from the plugin's configuration file in the plugins folder. Returning value signals
	 * whether this plugin was successfully configured / started and is ready for action.
	 * </p><p>
	 * <i>Reconfiguration</i><br>
	 * Reconfiguration of a loaded and running proxy plugin takes place only if plugin's
	 * configuration stored in it's configuration file but nothing else in the plugin's
	 * environment (plugin's roles or dependencies, shared libraries, shared services
	 * definitions) was changed since the plugin was loaded and started, and the plugin
	 * signals that it supports reconfiguration in it's current state
	 * (see {@link #supportsReconfigure()}).</p> 
	 * @param props configuration properties for this plugin
	 * @return <code>true</code> if the configuration and start was successful,
	 * <code>false</code> otherwise
	 */
	boolean start(PluginProperties props);
	
	/**
	 * This method is called to stop this proxy plugin. In this method the plugin should
	 * release all allocated resources.
	 */
	void stop();
}
