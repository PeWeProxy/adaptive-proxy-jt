package sk.fiit.rabbit.adaptiveproxy.plugins.services;

import java.util.List;
import sk.fiit.rabbit.adaptiveproxy.plugins.RequestPlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpRequest;

public interface RequestServicePlugin extends ServicePlugin, RequestPlugin {
	List<RequestServiceProvider> provideRequestServices(HttpRequest request);
}
