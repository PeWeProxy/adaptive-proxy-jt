package sk.fiit.rabbit.adaptiveproxy.plugins.services;

import sk.fiit.rabbit.adaptiveproxy.messages.ModifiableHttpRequest;

public interface RequestServiceProvider extends ServiceProvider {
	void setRequestContext(ModifiableHttpRequest request);
}
