package sk.fiit.peweproxy.messages;

import java.net.InetSocketAddress;

import sk.fiit.peweproxy.headers.HeaderWrapper;
import sk.fiit.peweproxy.services.ModulesManager;
import sk.fiit.peweproxy.services.RequestServiceHandleImpl;

public final class ModifiableHttpRequestImpl extends HttpMessageImpl<RequestServiceHandleImpl, ModifiableHttpRequest>
		implements ModifiableHttpRequest {
	private InetSocketAddress clientSocketAdr;
	private HeaderWrapper clientRQHeaders;
	private HeaderWrapper proxyRQHeaders;
	
	public ModifiableHttpRequestImpl(ModulesManager modulesManager, HeaderWrapper clientRQHeaders, InetSocketAddress clientSocketAdr) {
		this(modulesManager,clientRQHeaders
			,clientRQHeaders.clone(),clientSocketAdr);
	}
	
	private ModifiableHttpRequestImpl(ModulesManager modulesManager, HeaderWrapper clientRQHeaders,
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
	protected ModifiableHttpRequestImpl makeClone() {
		return new ModifiableHttpRequestImpl(getServicesHandle().getManager(), clientRQHeaders
				, new HeaderWrapper(proxyRQHeaders.getBackedHeader().clone()),clientSocketAdr);
	}
}
