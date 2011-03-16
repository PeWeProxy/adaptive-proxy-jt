package sk.fiit.peweproxy.services;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;

import rabbit.util.CharsetUtils;
import sk.fiit.peweproxy.headers.HeaderWrapper;
import sk.fiit.peweproxy.messages.HttpMessageImpl;
import sk.fiit.peweproxy.plugins.ProxyPlugin;

public abstract class MessageServicesHandle<ModuleType extends ProxyPlugin> extends
		ServicesHandleBase<ModuleType> {
	
	private boolean tempReadOnly = false;
	
	public MessageServicesHandle(HttpMessageImpl<?> httpMessage, List<ModuleType> modules, ModulesManager manager) {
		super(httpMessage, modules, manager);
	}
	
	@Override
	boolean dataAccessible() {
		return httpMessage.bodyAccessible();
	}

	@Override
	public boolean isReadOnly() {
		return tempReadOnly || httpMessage.isReadOnly();
	}
	
	void setReadOnly() {
		if (!tempReadOnly) // if tempReadOnly = TRUE, we are in finalize() and don't want to lock the message 
			httpMessage.setReadOnly();
	}
	
	public void setReadOnlyTemp(boolean readOnly) {
		tempReadOnly = readOnly;
	}
	
	@Override
	public byte[] getData() {
		return httpMessage.getData();
	}
	
	@Override
	public void setData(byte[] data) {
		httpMessage.setData(data);
	}
	
	@Override
	public void ceaseData(byte[] data) {
		throw new IllegalStateException("Calling ceaseContent() is illegal for full-message services handle");
	}
	
	@Override
	public Charset getCharset() throws UnsupportedCharsetException, IOException {
		return CharsetUtils.detectCharset(httpMessage.getHeader(), httpMessage.getData(), false);
	}
	
	@Override
	String getText4Logging(LogText type) {
		if (type == LogText.CONTENT_TYPE)
			return "message";
		if (type == LogText.SVC_NORMAL)
			return "message service";
		return null;
	}
	
	@Override
	HeaderWrapper getHeaderForPatternMatch() {
		return httpMessage.getHeader();
	}
}
