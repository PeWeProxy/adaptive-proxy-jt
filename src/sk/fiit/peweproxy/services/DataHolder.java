package sk.fiit.peweproxy.services;

/**
 * Data holder is a entity that is responsible for ceasing some data until it is joined with next incoming
 * message body chunk.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 *
 */
public interface DataHolder {
	/**
	 * Initiates storing of entities of particular service temporarily until next chunk is received, after what
	 * ceased data is joined with incoming chunk data followed by processing of resulting joined chunk.<br> 
	 * Calling this method makes AdaptiveProxy platform to cease bytes representing passed <i>chunkPart</i>
	 * object - entities in the context of particular service, by chunks service provider that provides passed
	 * <i>byService</i>. Transformation of entity to bytes is in the responsibility of the target service
	 * provider. It's the responsibility of the calling code to match type of the entity being ceased
	 * with type that target service's provider is able to process. 
	 * @param <T> type of the ceased entity
	 * @param <Service> particular proxy service type
	 * @param byService service implementation of which should cease data of the entity
	 * @param chunkPart entity recognized by the service (implementation)
	 */
	<T, Service extends ProxyService> void ceaseContent(Service byService, T chunkPart);
}
