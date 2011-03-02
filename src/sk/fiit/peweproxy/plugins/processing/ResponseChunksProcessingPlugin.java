package sk.fiit.peweproxy.plugins.processing;

import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.plugins.ProxyPlugin;
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
public interface ResponseChunksProcessingPlugin extends ProxyPlugin {
	
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
	 * from the web resource.
	 * @param response read-only response message containing already sent data
	 * @param chunkServiceshandle chunk services handle for full access to the remaining content
	 */
	void finalizeProcessing(HttpResponse response, ChunkServicesHandle chunkServicesHandle);
}
