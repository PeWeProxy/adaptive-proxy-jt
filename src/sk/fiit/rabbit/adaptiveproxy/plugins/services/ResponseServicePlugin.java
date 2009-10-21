package sk.fiit.rabbit.adaptiveproxy.plugins.services;

import java.util.List;
import sk.fiit.rabbit.adaptiveproxy.plugins.ResponsePlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpResponse;

public interface ResponseServicePlugin extends ServicePlugin, ResponsePlugin {
	List<ResponseServiceProvider> provideResponseServices(HttpResponse response);
}
