package sk.fiit.rabbit.adaptiveproxy.messages;

import java.net.InetSocketAddress;

import sk.fiit.rabbit.adaptiveproxy.headers.HeaderWrapper;
import sk.fiit.rabbit.adaptiveproxy.headers.ReadableHeader;
import sk.fiit.rabbit.adaptiveproxy.headers.WritableRequestHeader;
import sk.fiit.rabbit.adaptiveproxy.services.RequestServiceHandleImpl;
import sk.fiit.rabbit.adaptiveproxy.services.ServiceModulesManager;

public final class ModifiableHttpRequestImpl extends HttpMessageImpl<RequestServiceHandleImpl>
		implements ModifiableHttpRequest {
	private InetSocketAddress clientSocketAdr;
	private HeaderWrapper clientRQHeaders;
	private HeaderWrapper proxyRQHeaders;
	
	public ModifiableHttpRequestImpl(ServiceModulesManager modulesManager, HeaderWrapper clientRQHeaders, InetSocketAddress clientSocketAdr) {
		this.clientSocketAdr = clientSocketAdr;
		this.clientRQHeaders = clientRQHeaders;
		this.proxyRQHeaders = new HeaderWrapper(clientRQHeaders.getBackedHeader().clone());
		setServiceHandle(new RequestServiceHandleImpl(this,modulesManager));
	}
	
	@Override
	public HeaderWrapper getProxyRequestHeader() {
		return proxyRQHeaders;
	}

	@Override
	public HeaderWrapper getClientRequestHeader() {
		return clientRQHeaders;
	}

	@Override
	public InetSocketAddress getClientSocketAddress() {
		return clientSocketAdr;
	}

	@Override
	public ReadableHeader getOriginalHeader() {
		return clientRQHeaders;
	}

	@Override
	public WritableRequestHeader getProxyHeader() {
		return proxyRQHeaders;
	}
}
