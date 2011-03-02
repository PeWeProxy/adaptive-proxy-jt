package sk.fiit.peweproxy.plugins.services.impl.content;

import sk.fiit.peweproxy.plugins.services.impl.BaseServiceProvider;
import sk.fiit.peweproxy.services.ProxyService;

public abstract class BaseContentServiceProvider<Type, Service extends ProxyService>
	extends BaseServiceProvider<Type, Service> {
	
	protected final ServicesContentSource content;
	
	public BaseContentServiceProvider(ServicesContentSource content) {
		/*if (content.getData() == null)
			throw new IllegalStateException("No data in content");*/
		this.content = content;
	}
}
