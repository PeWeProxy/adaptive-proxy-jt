package sk.fiit.peweproxy.plugins.services.impl.content;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.UnsupportedCharsetException;

import rabbit.util.CharsetUtils;
import sk.fiit.peweproxy.messages.HttpMessageImpl;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.services.impl.BaseServiceProvider;
import sk.fiit.peweproxy.services.content.StringContentService;

public class StringServiceImpl extends BaseServiceProvider<StringContentService>
	implements StringContentService {
	
	final String content;
	
	public StringServiceImpl(HttpMessageImpl<?> httpMessage, boolean useJChardet)
		throws CharacterCodingException, UnsupportedCharsetException, IOException {
		super(httpMessage);
		byte[] data = httpMessage.getData();
		content = CharsetUtils.decodeBytes(data, CharsetUtils.detectCharset(httpMessage.getHeader(), data, useJChardet), true).toString();
		//MemoryUsageInspector.printMemoryUsage(log, "Before StringBuilder creation");
	}

	@Override
	public String getContent() {
		return content;
	}
	
	@Override
	public Class<StringContentService> getServiceClass() {
		return StringContentService.class;
	}
	
	@Override
	public void doChanges(ModifiableHttpRequest request) {}

	@Override
	public void doChanges(ModifiableHttpResponse response) {}
}
