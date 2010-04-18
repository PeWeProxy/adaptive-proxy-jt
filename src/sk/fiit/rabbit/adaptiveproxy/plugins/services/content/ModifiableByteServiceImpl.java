package sk.fiit.rabbit.adaptiveproxy.plugins.services.content;

import java.util.Arrays;

import sk.fiit.rabbit.adaptiveproxy.headers.ReadableHeader;
import sk.fiit.rabbit.adaptiveproxy.headers.WritableHeader;
import sk.fiit.rabbit.adaptiveproxy.messages.HttpMessageImpl;
import sk.fiit.rabbit.adaptiveproxy.messages.ModifiableHttpRequest;
import sk.fiit.rabbit.adaptiveproxy.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.services.content.ModifiableBytesService;

public class ModifiableByteServiceImpl<MessageType extends HttpMessageImpl<?>>
	extends BaseServiceProvider<MessageType, ModifiableBytesService> implements ModifiableBytesService {
	
	byte[] data = null;
	
	public ModifiableByteServiceImpl(MessageType httpMessage) {
		super(httpMessage);
		data = httpMessage.getData();
		data =  Arrays.copyOf(data, data.length);
	}
	
	@Override
	public byte[] getData() {
		return Arrays.copyOf(data, data.length);
	}
	
	@Override
	public void setData(byte[] data) {
		this.data = Arrays.copyOf(data, data.length); 
	}
	
	@Override
	public Class<ModifiableBytesService> getServiceClass() {
		return ModifiableBytesService.class;
	}
	
	public void doChanges(ReadableHeader origHeader, WritableHeader targetHeader) {
		httpMessage.setData(data);
		if (origHeader.getField("Content-Length") != null) {
			targetHeader.setField("Content-Length", Integer.toString(data.length, 10));
		}
	}

	@Override
	public void doChanges(ModifiableHttpRequest request) {
		doChanges(request.getClientRequestHeader(), request.getProxyRequestHeader());
	}

	@Override
	public void doChanges(ModifiableHttpResponse response) {
		doChanges(response.getWebResponseHeader(), response.getProxyResponseHeader());
	}
}
