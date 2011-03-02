package sk.fiit.peweproxy.plugins.services.impl.content;

import sk.fiit.peweproxy.services.ChunkServicesHandle;
import sk.fiit.peweproxy.services.ProxyService;

public abstract class BaseByteServicesProvider<Service extends ProxyService> extends
		BaseContentServiceProvider<byte[], Service> {
	
	public BaseByteServicesProvider(ServicesContentSource content) {
		super(content);
	}
	
	@Override
	public void ceaseContent(byte[] chunkPart, ChunkServicesHandle chunkServiceshandle) {
		if (chunkPart != null && chunkPart.length > 0)
			content.ceaseData(chunkPart);
	}
}
