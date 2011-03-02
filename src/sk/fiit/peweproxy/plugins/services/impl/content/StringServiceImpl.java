package sk.fiit.peweproxy.plugins.services.impl.content;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.UnsupportedCharsetException;

import sk.fiit.peweproxy.headers.HeaderWrapper;
import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.services.ServicesHandle;
import sk.fiit.peweproxy.services.content.StringContentService;

public class StringServiceImpl extends BaseStringServicesProvider<StringContentService>
	implements StringContentService {
	
	public StringServiceImpl(HeaderWrapper actualHeader, boolean useJChardet, ServicesContentSource content)
		throws CharacterCodingException, UnsupportedCharsetException, IOException {
		super(actualHeader, content, useJChardet);
	}

	@Override
	public String getContent() {
		return sb.toString();
	}
	
	@Override
	public Class<StringContentService> getServiceClass() {
		return StringContentService.class;
	}
	
	@Override
	public void doChanges(ModifiableHttpRequest request) {}

	@Override
	public void doChanges(ModifiableHttpResponse response) {}

	@Override
	public void doChanges(HttpRequest request, ServicesHandle chunkServicesHandle) {}

	@Override
	public void doChanges(HttpResponse response, ServicesHandle chunkServicesHandle) {}
}
