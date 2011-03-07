package sk.fiit.peweproxy.plugins.services.impl.content;

import sk.fiit.peweproxy.services.DataHolder;
import sk.fiit.peweproxy.services.ProxyService;

public abstract class BaseByteServicesProvider<Service extends ProxyService> extends
		BaseContentServiceProvider<byte[], Service> {
	
	public BaseByteServicesProvider(ServicesContentSource content) {
		super(content);
	}
	
	@Override
	public void ceaseContent(byte[] chunkPart, DataHolder dataHolder) {
		if (chunkPart != null && chunkPart.length > 0)
			content.ceaseData(chunkPart);
	}
}
