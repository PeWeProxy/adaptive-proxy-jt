package sk.fiit.peweproxy.plugins.processing;

import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.plugins.ProxyPlugin;
import sk.fiit.peweproxy.services.ChunkServicesHandle;

/**
 * Interface for proxy plugins involved in processing of chunks of HTTP requests.
 * <br><br>
 * <b>Proxy plugin interafce</b><br>
 * <i>This is an interface for one type of proxy plugins. Entities implementing this
 * interface are pluggable.</i>
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface RequestChunksProcessingPlugin extends ProxyPlugin {
	
	/**
	 * Process a request chunk accessible through passed <code>chunkServicesHandle</code>.
	 * Passed <code>request</code> makes data already sent to the web destination available,
	 * i.e. request's HTTP header and body data, chunks of which were already processed and
	 * sent to the web resource. Setting chunk data to non-null byte array (can also be empty
	 * array) signals AdaptiveProxy platform that it should proceed in chunk processing by
	 * other chunk plugins. Setting it to <code>null</code> signals that it should end chunk
	 * processing, send no data to the web destination and proceed in reading next request
	 * chunk.
	 * @param request read-only request message containing already sent data
	 * @param chunkServiceshandle chunk services handle for full access to the chunk data
	 */
	void processRequestChunk(HttpRequest request, ChunkServicesHandle chunkServicesHandle);
	
	/**
	 * Finalizes request's chunks processing by adding whatever this plugin needs to append
	 * (after the request) to the content accessible through passed <code>chunkServicesHandle
	 * </code>. This method is always run after processing of last body data chunk received
	 * from the client.
	 * @param request read-only request message containing already sent data
	 * @param chunkServiceshandle chunk services handle for full access to the remaining content
	 */
	void finalizeProcessing(HttpRequest request, ChunkServicesHandle chunkServicesHandle);
}
