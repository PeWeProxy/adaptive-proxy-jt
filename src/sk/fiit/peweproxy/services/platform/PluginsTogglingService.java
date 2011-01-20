package sk.fiit.peweproxy.services.platform;

import java.util.Set;

import sk.fiit.peweproxy.plugins.ProxyPlugin;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.ProxyService.messageIdependent;

/**
 * Plugins toggling service provides a mechanism to enable or disable integration of the
 * plugins to the process of handling of the messages of a particular user. AdaptiveProxy
 * platform will call this service (if available) to get blacklists - names of the plugins
 * (for each pluggable type) that will not be used in the handling of the messages initiated
 * by the same user (including one that the service is provided for).<br><br>
 * For the sake of performance, platform requests and uses realization of this service (if
 * available) only once for each individual user and then caches the resulting blaclists. If
 * there's a need to alter already cached blacklists, use {@link PlatformContextService}.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
@messageIdependent
public interface PluginsTogglingService extends ProxyService {

	public Set<String> getPluginsBlacklist(Class<? extends ProxyPlugin> pluginType);
}
