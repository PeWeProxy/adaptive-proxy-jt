package sk.fiit.peweproxy.plugins.services.impl;

import sk.fiit.peweproxy.messages.HttpMessageImpl;
import sk.fiit.peweproxy.plugins.services.RequestServiceProvider;
import sk.fiit.peweproxy.plugins.services.ResponseServiceProvider;
import sk.fiit.peweproxy.services.ProxyService;

public abstract class BaseServiceProvider<Service extends ProxyService>
	implements RequestServiceProvider<Service>, ResponseServiceProvider<Service>, ProxyService {
	protected final HttpMessageImpl<?> httpMessage;
	
	public BaseServiceProvider(HttpMessageImpl<?> httpMessage) {
		if (httpMessage == null)
			throw new IllegalStateException("HTTP message can not be null");
		if (httpMessage.getData() == null)
			throw new IllegalStateException("No data in message");
		this.httpMessage = httpMessage;
	}
	
	@Override
	public String getServiceIdentification() {
		return "AdaptiveProxy."+getServiceClass().getSimpleName();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Service getService() {
		return (Service)this;
	}
	
	@Override
	public boolean initChangedModel() {
		return false;
	}
	
	protected abstract Class<Service> getServiceClass();
}
