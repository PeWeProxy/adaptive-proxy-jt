package rabbit.httpio;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import rabbit.http.HttpHeader;
import rabbit.io.BufferHandle;
import rabbit.io.BufferHandler;
import rabbit.io.CacheBufferHandle;
import rabbit.util.Logger;
import rabbit.util.TrafficLogger;

/** A handler that write one http header and reads a response
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public class HttpResponseReader 
    implements HttpHeaderSentListener, HttpHeaderListener {

    private final SocketChannel channel;
    private final Selector selector;
    private final Logger logger;
    private final TrafficLogger tl;
    private final BufferHandler bufHandler;
    private final boolean strictHttp;
    private final HttpResponseListener listener;
    
    public HttpResponseReader (SocketChannel channel, Selector selector, 
			       Logger logger, TrafficLogger tl, 
			       BufferHandler bufHandler, 
			       HttpHeader header, boolean fullURI, 
			       boolean strictHttp, 
			       HttpResponseListener listener)
	throws IOException {
	this.channel = channel;
	this.selector = selector;
	this.logger = logger;
	this.tl = tl;
	this.bufHandler = bufHandler;
	this.strictHttp = strictHttp;
	this.listener = listener;
	new HttpHeaderSender (channel, selector, logger, tl, 
			      header, fullURI, this);
    }
    
    public void httpHeaderSent () {
	try {
	    BufferHandle bh = new CacheBufferHandle (bufHandler);
	    new HttpHeaderReader (channel, bh, selector, logger,
				  tl, false, strictHttp, this);
	} catch (IOException e) {
	    failed (e);
	}
    }
    
    public void httpHeaderRead (HttpHeader header, BufferHandle bh, 
				boolean keepalive, boolean isChunked, 
				long dataSize) {
	listener.httpResponse (header, bh, keepalive, isChunked, dataSize);
    }
    
    public void closed () {
	listener.failed (new IOException ("Connection closed"));
    }
    
    public void failed (Exception cause) {
	listener.failed (cause);
    }

    public void timeout () {
	listener.timeout ();
    }
}
