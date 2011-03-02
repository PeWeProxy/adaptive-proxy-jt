package sk.fiit.peweproxy.plugins.services.impl;

import sk.fiit.peweproxy.plugins.services.RequestChunkServiceProvider;
import sk.fiit.peweproxy.plugins.services.RequestServiceProvider;
import sk.fiit.peweproxy.plugins.services.ResponseChunkServiceProvider;
import sk.fiit.peweproxy.plugins.services.ResponseServiceProvider;
import sk.fiit.peweproxy.services.ProxyService;

public abstract class BaseServiceProvider<Type, Service extends ProxyService>
	implements RequestServiceProvider<Service>, ResponseServiceProvider<Service>, 
	RequestChunkServiceProvider<Type, Service>, ResponseChunkServiceProvider<Type, Service>,
	ProxyService {
	
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
