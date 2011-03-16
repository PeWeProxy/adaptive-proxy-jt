package sk.fiit.peweproxy.plugins.processing;

import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.ResponseChunksPlugin;
import sk.fiit.peweproxy.services.ChunkServicesHandle;

/**
 * Interface for proxy plugins involved in processing of chunks of HTTP responses.
 * <br><br>
 * <b>Proxy plugin interafce</b><br>
 * <i>This is an interface for one type of proxy plugins. Entities implementing this
 * interface are pluggable.</i>
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface ResponseChunksProcessingPlugin extends ResponseChunksPlugin {
	
	/**
	 * Processes passed <code>response</code> by this response chunks processing plugin. In this
	 * method a response chunks processing plugin can modify response header before it is sent
	 * to the client. Plugins are discouraged to use services accessing the data of the
	 * response's body in this method in rare situations when such services are available at
	 * this time.
	 * @param response full-access response message to process
	 */
	void startResponseProcessing(ModifiableHttpResponse response);
	
	/**
	 * Process a response chunk accessible through passed <code>chunkServicesHandle</code>.
	 * Passed <code>response</code> makes data already sent to the client available,
	 * i.e. response's HTTP header and body data, chunks of which were already processed and
	 * sent back to the client. Setting chunk data to non-null byte array (can also be empty
	 * array) signals AdaptiveProxy platform that it should proceed in chunk processing by
	 * other chunk plugins. Setting it to <code>null</code> signals that it should end chunk
	 * processing, send no data to the client and proceed in reading next response chunk. 
	 * @param response read-only response message containing already sent data
	 * @param chunkServiceshandle chunk services handle for full access to the chunk data
	 */
	void processResponseChunk(HttpResponse response, ChunkServicesHandle chunkServicesHandle);
	
	/**
	 * Finalizes response's chunks processing by adding whatever this plugin needs to append
	 * (after the response) to the content accessible through passed <code>chunkServicesHandle
	 * </code>. This method is always run after processing of last body data chunk received
	 * from the web resource. Setting chunk data to <code>null</code> does not break the process
	 * of finalizing of chunk processing by all plugins, thus this method is always called
	 * on every response chunks processing plugin.
	 * @param response read-only response message containing already sent data
	 * @param chunkServiceshandle chunk services handle for full access to the remaining content
	 */
	void finalizeResponseProcessing(HttpResponse response, ChunkServicesHandle chunkServicesHandle);
}
