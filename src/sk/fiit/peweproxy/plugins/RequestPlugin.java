package sk.fiit.peweproxy.plugins;

import java.util.Set;

import sk.fiit.peweproxy.headers.RequestHeader;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.content.ByteContentService;

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
	 * Returns set of services (their classes) over request message that this plugin wishes to
	 * be able to obtain and use later when it will be involved in the request handling process.
	 * This set is used to decide whether there is a need for the basic data service
	 * ({@link ByteContentService}) and the proxy server should precache all request body data.
	 * @param clientRQHeaders read-only request headers
	 * @return set of service classes that plugin wishes to have later in the processing phase
	 */
	Set<Class<? extends ProxyService>> desiredRequestServices(RequestHeader clientRQHeaders);
}
