package sk.fiit.peweproxy.services.platform;

import java.util.Set;

import sk.fiit.peweproxy.plugins.ProxyPlugin;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ProxyService.readonly;

/**
 * Plugins toggling service provides AdaptiveProxy platform with the blacklist of plugins
 * integration of which is disabled at specific places of the message handling process for the
 * particular user. AdaptiveProxy platform will call this service (if available) to get
 * blacklists - names of the plugins (for each pluggable type) that will not be used in the
 * handling of the messages initiated by the same user (including one that the service is
 * provided for).<br><br>
 * For the sake of performance, platform requests and uses realizations of this service (if
 * available) only once for each individual user and then caches the resulting blaclists. If
 * there's a need to alter already cached blacklists, use {@link PlatformContextService}.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
@readonly
@SuppressWarnings("all") // to avoid compile time warning
public interface PluginsTogglingService extends ProxyService {

	/**
	 * Returns the set of names of the plugins that are disabled for being used as particular
	 * plugin type specified by passed <code>pluginType</code> in the process of handling HTTP
	 * messages from the user that initiated the message this service is provided for.
	 * @param pluginType type of the proxy plugin to get disabled plugins for
	 * @return set of names of disabled plugins for specific type
	 */
	public Set<String> getPluginsBlacklist(Class<? extends ProxyPlugin> pluginType);
}
