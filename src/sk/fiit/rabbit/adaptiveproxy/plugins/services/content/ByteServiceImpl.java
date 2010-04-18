package sk.fiit.rabbit.adaptiveproxy.plugins.services.content;

import java.util.Arrays;

import sk.fiit.rabbit.adaptiveproxy.messages.HttpMessageImpl;
import sk.fiit.rabbit.adaptiveproxy.messages.ModifiableHttpRequest;
import sk.fiit.rabbit.adaptiveproxy.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.services.content.ByteContentService;

public class ByteServiceImpl<MessageType extends HttpMessageImpl<?>>
	extends BaseServiceProvider<MessageType,ByteContentService> implements ByteContentService {
	
	public ByteServiceImpl(MessageType httpMessage) {
		super(httpMessage);
	}
	
	@Override
	public byte[] getData() {
		byte[] data = httpMessage.getData();
		return Arrays.copyOf(data, data.length);
	}
	
	@Override
	public Class<ByteContentService> getServiceClass() {
		return ByteContentService.class;
	}

	@Override
	public void doChanges(ModifiableHttpRequest request) {}

	@Override
	public void doChanges(ModifiableHttpResponse response) {}
}
