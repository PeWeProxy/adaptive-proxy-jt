package sk.fiit.peweproxy.plugins.services.content;

import java.util.Arrays;

import sk.fiit.peweproxy.messages.HttpMessageImpl;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.services.content.ByteContentService;

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
