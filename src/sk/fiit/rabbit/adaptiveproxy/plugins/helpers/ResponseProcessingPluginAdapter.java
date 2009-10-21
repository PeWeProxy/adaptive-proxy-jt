package sk.fiit.rabbit.adaptiveproxy.plugins.helpers;

import sk.fiit.rabbit.adaptiveproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.headers.ResponseHeaders;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpMessageFactory;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.HttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.processing.ResponseProcessingPlugin;

public class ResponseProcessingPluginAdapter implements ResponseProcessingPlugin {

	@Override
	public HttpResponse getNewResponse(ModifiableHttpResponse response,
			HttpMessageFactory messageFactory) {
		return null;
	}

	@Override
	public ResponseProcessingActions processResponse(
			ModifiableHttpResponse response) {
		return null;
	}

	@Override
	public boolean wantResponseContent(ResponseHeaders webRPHeaders) {
		return false;
	}

	@Override
	public boolean setup(PluginProperties props) {
		return true;
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public boolean supportsReconfigure() {
		return true;
	}
}
