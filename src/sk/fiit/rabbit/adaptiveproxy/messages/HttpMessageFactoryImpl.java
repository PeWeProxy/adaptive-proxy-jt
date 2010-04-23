package sk.fiit.rabbit.adaptiveproxy.messages;

import java.util.Date;
import org.apache.log4j.Logger;
import rabbit.http.HttpDateParser;
import rabbit.http.HttpHeader;
import rabbit.proxy.Connection;
import sk.fiit.rabbit.adaptiveproxy.AdaptiveEngine;
import sk.fiit.rabbit.adaptiveproxy.headers.HeaderWrapper;
import sk.fiit.rabbit.adaptiveproxy.utils.HeaderUtils;

public final class HttpMessageFactoryImpl implements HttpMessageFactory {
	private static final Logger log = Logger.getLogger(HttpMessageFactoryImpl.class.getName());
	
	private final AdaptiveEngine adaptiveEngine;
	private final Connection con;
	private final ModifiableHttpRequestImpl request;
	
	public HttpMessageFactoryImpl(AdaptiveEngine adaptiveEngine, Connection con, ModifiableHttpRequestImpl request) {
		this.adaptiveEngine = adaptiveEngine;
		this.con = con;
		this.request = request;
	}
	
	@Override
	public ModifiableHttpRequest constructHttpRequest(ModifiableHttpRequest baseRequest, String contentType) {
		ModifiableHttpRequestImpl retVal = null;
		if (baseRequest != null) {
			retVal = ((ModifiableHttpRequestImpl)baseRequest).clone();
		} else {
			retVal = new ModifiableHttpRequestImpl(adaptiveEngine.getModulesManager(),new HeaderWrapper(new HttpHeader())
			,(request != null) ? request.getClientSocketAddress() : null);
		}
		retVal.setAllowedThread();
		HttpHeader proxyHeader = retVal.getProxyHeader().getBackedHeader();
		if (contentType != null) {
			proxyHeader.setHeader ("Content-Type", contentType);
			// TODO skontrolovat ci toto neurobi pruser potom pri posielani (hint: chunking )
			proxyHeader.setHeader ("Content-Length", "0");
			retVal.setData(new byte[0]);
		} else
			HeaderUtils.removeContentHeaders(proxyHeader);
		return retVal;
	}

	@Override
	public ModifiableHttpResponse constructHttpResponse(ModifiableHttpResponse baseResponse, String contentType) {
		ModifiableHttpResponseImpl retVal = null;
		if (baseResponse != null)
			retVal = ((ModifiableHttpResponseImpl)baseResponse).clone();
		else {
			retVal = new ModifiableHttpResponseImpl(adaptiveEngine.getModulesManager()
						,new HeaderWrapper(new HttpHeader()),request);
			HttpHeader header = retVal.getProxyHeader().getBackedHeader();
			header.setStatusLine("HTTP/1.1 200 OK");
			header.setHeader("Date", HttpDateParser.getDateString(new Date()));
			header.setHeader("Via", con.getProxy().getProxyIdentity());
			HttpHeader filteredHeaders =  con.filterConstructedResponse(header);
			if (filteredHeaders != null)
				log.debug("If this was a normaly received response, it would have been blocked by header filters");
				// but now we don't care
		}
		retVal.setAllowedThread();
		HttpHeader proxyHeader = retVal.getProxyHeader().getBackedHeader();
		if (contentType != null) {
			proxyHeader = retVal.getProxyHeader().getBackedHeader();
			proxyHeader.setHeader ("Content-Type", contentType);
			// TODO skontrolovat ci toto neurobi pruser potom pri posielani (hint: chunking )
			proxyHeader.setHeader ("Content-Length", "0");
			retVal.setData(new byte[0]);
		} else
			HeaderUtils.removeContentHeaders(proxyHeader);
		return retVal;
	}
}
