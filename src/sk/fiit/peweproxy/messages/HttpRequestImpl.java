package sk.fiit.peweproxy.messages;


import java.net.InetSocketAddress;

import sk.fiit.peweproxy.headers.HeaderWrapper;
import sk.fiit.peweproxy.headers.RequestHeader;
import sk.fiit.peweproxy.services.ModulesManager;
import sk.fiit.peweproxy.services.RequestServiceHandleImpl;

public class HttpRequestImpl extends HttpMessageImpl<RequestServiceHandleImpl>
	implements HttpRequest {
	
	private InetSocketAddress clientSocketAdr;

	public HttpRequestImpl(ModulesManager modulesManager, HeaderWrapper header,
			InetSocketAddress clientSocketAdr) {
		super(header);
		this.clientSocketAdr = clientSocketAdr;
		setServicesHandle(new RequestServiceHandleImpl(this,modulesManager));
	}
	
	@Override
	public RequestHeader getRequestHeader() {
		return header;
	}
	
	@Override
	public HttpRequest getOriginalRequest() {
		// this class is for representations of original requests, let them
		// point to self when asked for original message 
		return this;
	}

	@Override
	public InetSocketAddress getClientSocketAddress() {
		return clientSocketAdr;
	}
	
	public InetSocketAddress clientSocketAddress() {
		return clientSocketAdr;
	}
	
	@Override
	public HttpRequestImpl clone() {
		return clone(new HttpRequestImpl(getServicesHandle().getManager(),
				header.clone(), clientSocketAdr));
	}
}
