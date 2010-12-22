package sk.fiit.peweproxy.plugins.services.impl.processing;

import java.util.Map;

import sk.fiit.peweproxy.AdaptiveEngine;
import sk.fiit.peweproxy.messages.HttpMessageImpl;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.services.impl.BaseServiceProvider;
import sk.fiit.peweproxy.services.processing.PluginsTogglingService;

public class PluginsTogglingServiceImpl extends BaseServiceProvider<PluginsTogglingService> implements
		PluginsTogglingService {
	
	final AdaptiveEngine adaptiveEngine;
	final String userId;
	
	public PluginsTogglingServiceImpl(HttpMessageImpl<?> httpMessage, AdaptiveEngine adaptiveEngine,
			String userId) {
		super(httpMessage);
		this.adaptiveEngine = adaptiveEngine;
		this.userId = userId;
	}

	@Override
	public void doChanges(ModifiableHttpRequest request) {}

	@Override
	public void doChanges(ModifiableHttpResponse response) {}

	@Override
	public Map<String, Boolean> getRequestPluginsStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Boolean> getResponsePluginsStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setRequestPluginsStatus(Map<String, Boolean> newStatuses) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setResponsePluginsStatus(Map<String, Boolean> newStatuses) {
		// TODO Auto-generated method stub
	}

	@Override
	protected Class<PluginsTogglingService> getServiceClass() {
		return PluginsTogglingService.class;
	}
}
