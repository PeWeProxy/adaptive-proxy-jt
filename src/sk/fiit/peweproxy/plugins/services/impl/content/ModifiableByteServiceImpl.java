package sk.fiit.peweproxy.plugins.services.impl.content;

import java.util.Arrays;

import sk.fiit.peweproxy.headers.ReadableHeader;
import sk.fiit.peweproxy.headers.WritableHeader;
import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.services.ServicesHandle;
import sk.fiit.peweproxy.services.content.ModifiableBytesService;

public class ModifiableByteServiceImpl extends BaseByteServicesProvider<ModifiableBytesService>
	implements ModifiableBytesService {
	
	byte[] data = null;
	
	public ModifiableByteServiceImpl(ServicesContentSource content) {
		super(content);
		data = content.getData();
		data =  Arrays.copyOf(data, data.length);
	}
	
	@Override
	public byte[] getData() {
		return Arrays.copyOf(data, data.length);
	}
	
	@Override
	public void setData(byte[] data) {
		if (data != null)
			this.data = Arrays.copyOf(data, data.length);
		else
			this.data = null;
	}
	
	@Override
	public Class<ModifiableBytesService> getServiceClass() {
		return ModifiableBytesService.class;
	}
	
	private void doChanges(ReadableHeader origHeader, WritableHeader targetHeader) {
		content.setData(data);
		if (origHeader != null && origHeader.getField("Content-Length") != null) {
			targetHeader.setField("Content-Length", Integer.toString(data.length, 10));
		}
	}

	@Override
	public void doChanges(ModifiableHttpRequest request) {
		doChanges(request.getOriginalRequest().getRequestHeader(), request.getRequestHeader());
	}

	@Override
	public void doChanges(ModifiableHttpResponse response) {
		doChanges(response.getOriginalResponse().getResponseHeader(), response.getResponseHeader());
	}

	@Override
	public void doChanges(HttpRequest request, ServicesHandle chunkServicesHandle) {
		doChanges((ReadableHeader)null, (WritableHeader)null);
	}

	@Override
	public void doChanges(HttpResponse response, ServicesHandle chunkServicesHandle) {
		doChanges((ReadableHeader)null, (WritableHeader)null);
	}
}
