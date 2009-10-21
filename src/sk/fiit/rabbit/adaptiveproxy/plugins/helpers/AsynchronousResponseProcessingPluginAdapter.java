package sk.fiit.rabbit.adaptiveproxy.plugins.helpers;

import java.util.HashMap;
import java.util.Map;

import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.ServicesHandle;

public abstract class AsynchronousResponseProcessingPluginAdapter extends ResponseProcessingPluginAdapter {
	
	private Map<String, Object> cache = new HashMap<String, Object>();
	
	protected final void addToCache(String name, Object obj) {
		cache.put(name, obj);
	}
	
	protected final Object getFromCache(String name) {
		return cache.get(name);
	}
	
	@Override
	public final ResponseProcessingActions processResponse(final ModifiableHttpResponse response) {
		if(!prepareServices(response.getServiceHandle())) {
			return ResponseProcessingActions.PROCEED;
		}
		
		new Thread() {
			@Override
			public void run() {
				processResponseAsynchronously(response);
			}
		}.start();
		
		return ResponseProcessingActions.PROCEED;
	}
	
	protected abstract boolean prepareServices(ServicesHandle servicesHandle);

	public abstract void processResponseAsynchronously(ModifiableHttpResponse response);
}
