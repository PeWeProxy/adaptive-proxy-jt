package sk.fiit.rabbit.adaptiveproxy.plugins.services.content;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.UnsupportedCharsetException;

import rabbit.util.CharsetUtils;
import sk.fiit.rabbit.adaptiveproxy.messages.HttpMessageImpl;
import sk.fiit.rabbit.adaptiveproxy.messages.ModifiableHttpRequest;
import sk.fiit.rabbit.adaptiveproxy.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.services.content.StringContentService;

public class StringServiceImpl<MessageType extends HttpMessageImpl<?>>
extends BaseServiceProvider<MessageType,StringContentService> implements StringContentService {
	
	final String content;
	
	public StringServiceImpl(MessageType httpMessage, boolean useJChardet)
		throws CharacterCodingException, UnsupportedCharsetException, IOException {
		super(httpMessage);
		byte[] data = httpMessage.getData();
		content = CharsetUtils.decodeBytes(data, CharsetUtils.detectCharset(httpMessage.getProxyHeader(), data, useJChardet), true).toString();
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
