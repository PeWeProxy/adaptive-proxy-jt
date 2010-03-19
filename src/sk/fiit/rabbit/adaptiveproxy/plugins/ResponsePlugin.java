package sk.fiit.rabbit.adaptiveproxy.plugins;

import java.util.Set;

import sk.fiit.rabbit.adaptiveproxy.headers.ResponseHeader;
import sk.fiit.rabbit.adaptiveproxy.services.ProxyService;
import sk.fiit.rabbit.adaptiveproxy.services.content.ByteContentService;

/**
 * Base interface for proxy plugins that are involved in the process of handling HTTP responses.
 * This interface defines only one method used to decide whether response body data should be
 * precached before processing by plugins or not (see
 * {@link #desiredResponseServices(ResponseHeader)}).
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ResponsePlugin extends ProxyPlugin {
	/**
	 * Returns set of services (their classes) over response message that this plugin wishes to
	 * be able to obtain and use later when it will be involved in the response handling process.
	 * This set is used to decide whether there is a need for the basic data service
	 * ({@link ByteContentService}) and the proxy server should precache all response body data.
	 * @param webRPHeaders read-only response headers
	 * @return set of service classes that plugin wishes to have later in the processing phase
	 */
	Set<Class<? extends ProxyService>> desiredResponseServices(ResponseHeader webRPHeaders);
}
