package sk.fiit.peweproxy.plugins.services.impl.content;

import java.util.Arrays;

import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.services.ServicesHandle;
import sk.fiit.peweproxy.services.content.ByteContentService;

public class ByteServiceImpl extends BaseByteServicesProvider<ByteContentService>
	implements ByteContentService {	
	public ByteServiceImpl(ServicesContentSource content) {
		super(content);
	}
	
	@Override
	public byte[] getData() {
		byte[] data = content.getData();
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

	@Override
	public void doChanges(HttpRequest request, ServicesHandle chunkServicesHandle) {}

	@Override
	public void doChanges(HttpResponse response, ServicesHandle chunkServicesHandle) {}
}
