package rabbit.webserver;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.SocketChannel;
import java.util.Date;
import rabbit.http.HttpHeader;
import rabbit.http.HttpDateParser;
import rabbit.httpio.FileResourceSource;
import rabbit.httpio.HttpHeaderListener;
import rabbit.httpio.HttpHeaderReader;
import rabbit.httpio.HttpHeaderSender;
import rabbit.httpio.HttpHeaderSentListener;
import rabbit.httpio.ResourceSource;
import rabbit.httpio.SelectorRunner;
import rabbit.httpio.TransferHandler;
import rabbit.httpio.TransferListener;
import rabbit.io.BufferHandle;
import rabbit.io.CacheBufferHandle;
import rabbit.util.MimeTypeMapper;
import rabbit.util.TrafficLogger;

/** A connection to a web client.
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public class Connection {
    private SimpleWebServer sws;
    private SocketChannel sc;
    private BufferHandle clientBufferHandle;
    private boolean timeToClose = false;
    private ResourceSource resourceSource = null;

    /** Create a new Connection for the given web server and socket channel. */
    public Connection (SimpleWebServer sws, SocketChannel sc) {
	this.sws = sws;
	this.sc = sc;
    }

    /** Set up a http reader to listen for http request. */
    public void readRequest () throws IOException {
	if (clientBufferHandle == null)
	    clientBufferHandle = 
		new CacheBufferHandle (sws.getBufferHandler ());
	HttpHeaderListener requestListener = new RequestListener ();
	new HttpHeaderReader (sc, clientBufferHandle, 
			      sws.getSelectorRunner ().getSelector (),
			      sws.getLogger (), 
			      sws.getTrafficLogger (), true,
			      true, requestListener);
    }

    private void shutdown () {
	try {
	    sc.close ();
	} catch (IOException e) {
	    sws.getLogger ().logWarn ("Failed to close down connection: " + e);
	}
    }
	
    private void handleRequest (HttpHeader header) {
	String method = header.getMethod ();
	if ("GET".equals (method) || "HEAD".equals (method)) {
	    String path = header.getRequestURI ();
	    if (path == null || "".equals (path)) {
		badRequest ();
		return;
	    } 
	    try {
		if (!path.startsWith ("/")) {
		    URL u = new URL (path);
		    path = u.getFile ();
		}
		if (path.endsWith ("/"))
		    path += "index.html";
		path = path.substring (1);
		File f = new File (sws.getBaseDir (), path);
		f = f.getCanonicalFile ();
		if (isSafe (f) && f.exists () && f.isFile ()) {
		    HttpHeader resp = getHeader ("HTTP/1.1 200 Ok");
		    String type = MimeTypeMapper.getMimeType (f.getAbsolutePath ());
		    if (type != null)
			resp.setHeader ("Content-Type", type);
		    resp.setHeader ("Content-Length", 
				    Long.toString (f.length ()));
		    Date d = new Date (f.lastModified ());
		    resp.setHeader ("Last-Modified", 
				    HttpDateParser.getDateString (d));
		    if ("HTTP/1.0".equals (header.getHTTPVersion ()))
			resp.setHeader ("Connection", "Keep-Alive");
		    if ("GET".equals (method))
			resourceSource = 
			    new FileResourceSource (f, sws.getSelectorRunner (),
						    sws.getBufferHandler ());
		    sendResponse (resp);
		} else {
		    notFound ();
		}
	    } catch (IOException e) {
		internalError ();
	    }
	} else {
	    methodNotAllowed ();
	}
    }

    private boolean isSafe (File f) {
	File dir = sws.getBaseDir ();
	return f.getAbsolutePath ().startsWith (dir.getAbsolutePath ());
    }

    private void notFound () {
	sendResponse (getHeader ("HTTP/1.1 404 Not Found"));
    }

    private void badRequest () {
	sendBadResponse (getHeader ("HTTP/1.1 400 Bad Request"));
    }
	
    private void methodNotAllowed () {
	sendBadResponse (getHeader ("HTTP/1.1 405 Method Not Allowed"));
    }

    private void internalError () {
	sendBadResponse (getHeader ("HTTP/1.1 500 Internal Error"));
    }

    private void notImplemented () {
	sendBadResponse (getHeader ("HTTP/1.1 501 Not Implemented"));
    }

    private void sendBadResponse (HttpHeader response) {
	timeToClose = true;
	sendResponse (response);
    }

    private void sendResponse (HttpHeader response) {
	try {
	    ResponseSentListener sentListener = 
		new ResponseSentListener ();
	    new HttpHeaderSender (sc, sws.getSelectorRunner ().getSelector (),
				  sws.getLogger (), 
				  sws.getTrafficLogger (), response, 
				  false, sentListener);
	} catch (IOException e) {
	    shutdown ();
	}
    }

    private void sendResource () {
	TransferListener transferDoneListener = 
	    new TransferDoneListener ();
	TrafficLogger tl = sws.getTrafficLogger ();
	SelectorRunner sr = sws.getSelectorRunner ();
	TransferHandler th = 
	    new TransferHandler (sr, resourceSource, sr.getSelector (), 
				 sc, tl, tl, transferDoneListener, 
				 sws.getLogger ());
	th.transfer ();
    }

    private HttpHeader getHeader (String statusLine) {
	HttpHeader ret = new HttpHeader ();
	ret.setStatusLine (statusLine);
	ret.setHeader ("Server", sws.getClass ().getName ());
	ret.setHeader ("Content-type", "text/html");
	ret.setHeader ("Date", HttpDateParser.getDateString (new Date ()));
	return ret;    
    }

    private void closeOrContinue () {
	if (timeToClose) {
	    shutdown ();
	} else {
	    try {
		readRequest ();
	    } catch (IOException e) {
		shutdown ();
	    }
	}
    }

    private class AsyncBaseListener {
	public void timeout () {
	    shutdown ();
	}
	    
	public void failed (Exception e) {
	    shutdown ();
	}
    }
	
    private class RequestListener extends AsyncBaseListener 
	implements HttpHeaderListener {
	public void httpHeaderRead (HttpHeader header, BufferHandle bh, 
				    boolean keepalive, boolean isChunked, 
				    long dataSize) {
	    bh.possiblyFlush ();
	    if (isChunked || dataSize > 0)
		notImplemented ();
	    if (!keepalive)
		timeToClose = true;
	    handleRequest (header);
	}

	public void closed () {
	    shutdown ();
	}
    }

    private class ResponseSentListener extends AsyncBaseListener 
	implements HttpHeaderSentListener {
	public void httpHeaderSent () {
	    if (resourceSource != null)
		sendResource ();
	    else  
		closeOrContinue ();
	}
    }

    private class TransferDoneListener extends AsyncBaseListener 
	implements TransferListener {
	public void transferOk () {
	    resourceSource.release ();
	    closeOrContinue ();	
	}
    }
}
