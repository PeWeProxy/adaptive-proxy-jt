package sk.fiit.peweproxy.plugins.processing;

import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.plugins.RequestChunksPlugin;
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
public interface RequestChunksProcessingPlugin extends RequestChunksPlugin {
	
	/**
	 * Processes passed <code>request</code> by this request chunks processing plugin. In this
	 * method a request chunks processing plugin can modify request header before it is sent
	 * to the web resource. Plugins are discouraged to use services accessing the data of the
	 * request's body in this method in rare situations when such services are available at
	 * this time.
	 * @param request full-access request message to process
	 */
	void startRequestProcessing(ModifiableHttpRequest request);
	
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
	 * @param chunkServicesHandle chunk services handle for full access to the chunk data
	 */
	void processRequestChunk(HttpRequest request, ChunkServicesHandle chunkServicesHandle);
	
	/**
	 * Finalizes request's chunks processing by adding whatever this plugin needs to append
	 * (after the request) to the content accessible through passed <code>chunkServicesHandle
	 * </code>. This method is always run after processing of last body data chunk received
	 * from the client. Setting chunk data to <code>null</code> does not break the process
	 * of finalizing of chunk processing by all plugins, thus this method is always called
	 * on every request chunks processing plugin.
	 * @param request read-only request message containing already sent data
	 * @param chunkServicesHandle chunk services handle for full access to the remaining content
	 */
	void finalizeRequestProcessing(HttpRequest request, ChunkServicesHandle chunkServicesHandle);
}
