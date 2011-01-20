package sk.fiit.peweproxy.plugins.services.impl.content;

import sk.fiit.peweproxy.messages.HttpMessageImpl;
import sk.fiit.peweproxy.plugins.services.impl.BaseServiceProvider;
import sk.fiit.peweproxy.services.ProxyService;

public abstract class BaseMessageServiceProvider<Service extends ProxyService>
	extends BaseServiceProvider<Service> {
	
	public BaseMessageServiceProvider(HttpMessageImpl<?> httpMessage) {
		super(httpMessage);
		if (httpMessage.getData() == null)
			throw new IllegalStateException("No data in message");
	}
}
