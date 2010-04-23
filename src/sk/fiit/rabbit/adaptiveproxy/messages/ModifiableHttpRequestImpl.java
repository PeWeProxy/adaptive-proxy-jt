package sk.fiit.rabbit.adaptiveproxy.messages;

import java.net.InetSocketAddress;

import sk.fiit.rabbit.adaptiveproxy.headers.HeaderWrapper;
import sk.fiit.rabbit.adaptiveproxy.services.RequestServiceHandleImpl;
import sk.fiit.rabbit.adaptiveproxy.services.ServiceModulesManager;

public final class ModifiableHttpRequestImpl extends HttpMessageImpl<RequestServiceHandleImpl>
		implements ModifiableHttpRequest {
	private InetSocketAddress clientSocketAdr;
	private HeaderWrapper clientRQHeaders;
	private HeaderWrapper proxyRQHeaders;
	
	public ModifiableHttpRequestImpl(ServiceModulesManager modulesManager, HeaderWrapper clientRQHeaders, InetSocketAddress clientSocketAdr) {
		this(modulesManager,clientRQHeaders
			,clientRQHeaders.clone(),clientSocketAdr);
	}
	
	private ModifiableHttpRequestImpl(ServiceModulesManager modulesManager, HeaderWrapper clientRQHeaders,
			HeaderWrapper proxyRQHeaders, InetSocketAddress clientSocketAdr) {
		this.clientSocketAdr = clientSocketAdr;
		this.clientRQHeaders = clientRQHeaders;
		this.proxyRQHeaders = proxyRQHeaders;
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
	public HeaderWrapper getOriginalHeader() {
		return clientRQHeaders;
	}

	@Override
	public HeaderWrapper getProxyHeader() {
		return proxyRQHeaders;
	}
	
	@Override
	public ModifiableHttpRequestImpl clone() {
		ModifiableHttpRequestImpl retVal =  new ModifiableHttpRequestImpl(getServiceHandle().getManager(), clientRQHeaders
				, new HeaderWrapper(proxyRQHeaders.getBackedHeader().clone()),clientSocketAdr);
		retVal.disableThreadCheck();
		return retVal;
	}
}
