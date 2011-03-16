package sk.fiit.peweproxy.plugins;

import java.util.Set;

import sk.fiit.peweproxy.headers.RequestHeader;
import sk.fiit.peweproxy.services.ProxyService;

/**
 * Base interface for chunks proxy plugins that are involved in processing of chunks of HTTP responses.
 * This interface defines only one method used to decide whether incoming responses that are not
 * chunked should be sent with chunked transfer coding thus enabling responses' chunks processing
 * functionality (see {@link #desiredResponseChunkServices(Set, RequestHeader)}).
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ResponseChunksPlugin extends ProxyPlugin {
	/**
	 * Called by platform to get services (their classes) over request message chunks that this
	 * plugin wishes to be able to get and use later during request's chunks processing. This set
	 * is used to decide whether the platform needs to modify the header of the request in such
	 * way that will enable processing of the chunks of the request. For convenience, an empty set
	 * is passed so that a plugin only needs to fill it with desired services (their interfaces).
	 * @param desiredServices set to be filled with classes of desired services
	 * @param clientRQHeader read-only request header
	 */
	void desiredResponseChunkServices(Set<Class<? extends ProxyService>> desiredServices,
			RequestHeader clientRQHeader);
}
