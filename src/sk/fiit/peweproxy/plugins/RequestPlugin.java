package sk.fiit.peweproxy.plugins;

import java.util.Set;

import sk.fiit.peweproxy.headers.RequestHeader;
import sk.fiit.peweproxy.services.ProxyService;

/**
 * Base interface for proxy plugins that are involved in the process of handling HTTP requests.
 * This interface defines only one method used to decide whether request body data should be
 * precached before processing by plugins or not (see
 * {@link #desiredRequestServices(RequestHeader)}).
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface RequestPlugin extends ProxyPlugin {
	/**
	 * Called by platform to get services (their classes) over request message that this plugin
	 * wishes to be able to get and use later when it will be involved in the request handling
	 * process. This set is used to decide whether there is a need for the basic data services
	 * and the proxy server should precache all request body data. For convenience, an empty
	 * set is passed so that a plugin only needs to fill it with desired services (their
	 * interfaces).
	 * @param desiredServices set to be filled with classes of desired services
	 * @param clientRQHeaders read-only request header
	 */
	void desiredRequestServices(Set<Class<? extends ProxyService>> desiredServices,
			RequestHeader clientRQHeader);
}
