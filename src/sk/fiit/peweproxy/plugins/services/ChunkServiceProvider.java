package sk.fiit.peweproxy.plugins.services;

import sk.fiit.peweproxy.services.DataHolder;
import sk.fiit.peweproxy.services.ProxyService;

public interface ChunkServiceProvider<Type, Service extends ProxyService> extends ServiceProvider<Service> {
	/**
	 * Store data representing passed <i>chunkPart</i> for later joining with data of the next chunk. This
	 * method is called when some plugin asks AdaptiveProxy platform to cease entity by a service this
	 * chunk service provider provides. This provider should initiate ceasing of underlying data that forms
	 * passed <i>chunkPart</i> by services that provided it with such data.
	 * @param chunkPart entity in the context of a service that need to be ceased
	 * @param dataHolder data holder that handles ceasing of underlying data
	 * @see DataHolder#ceaseContent(ProxyService, Object)
	 */
	void ceaseContent(Type chunkPart, DataHolder dataHolder);
}
