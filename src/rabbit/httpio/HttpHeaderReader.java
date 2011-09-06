package rabbit.httpio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.khelekore.rnio.NioHandler;
import org.khelekore.rnio.ReadHandler;
import rabbit.http.HttpHeader;
import rabbit.io.BufferHandle;
import rabbit.util.TrafficLogger;

/** A handler that reads http headers
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public class HttpHeaderReader extends BaseSocketHandler
    implements ReadHandler {
    
    private final HttpHeaderListener reader;
    private final HttpHeaderParser headerParser;

    // State variables.
    
    private int startParseAt = 0;

    private final TrafficLogger tl;

    /** 
     * @param channel the SocketChannel to read from
     * @param bh the BufferHandle to use to get ByteBuffers
     * @param nioHandler the NioHandler to use to wait for more data
     * @param tl the TrafficLogger to update with network read Statistics
     * @param request true if a request is read, false if a response is read.
     *                Servers may respond without header (HTTP/0.9) so try to 
     *                handle that.
     * @param strictHttp if true http headers will be strictly parsed, if false
     *                   http newlines may be single \n
     * @param reader the listener for http headers
     */ 
    public HttpHeaderReader (SocketChannel channel, BufferHandle bh, 
			     NioHandler nioHandler, TrafficLogger tl, 
			     boolean request, boolean strictHttp, 
			     HttpHeaderListener reader) {
	super (channel, bh, nioHandler);
	this.tl = tl;
	headerParser = new HttpHeaderParser (request, strictHttp);
	this.reader = reader;
    }

    /** Try to read a http header
     * @throws IOException if a header can not be parsed
     */
    public void readHeader () throws IOException {
	if (!getBufferHandle ().isEmpty ()) {
	    ByteBuffer buffer = getBuffer ();
	    startParseAt = buffer.position ();
	    parseBuffer (buffer);
	} else {
	    releaseBuffer ();
	    waitForRead (this);
	}
    }

    @Override public String getDescription () {
	HttpHeader header = headerParser.getHeader ();
	return "HttpHeaderReader: channel: " + getChannel () + 
	    ", current header lines: " + 
	    (header == null ? 0 : header.size ());
    }

    @Override public void closed () {
	releaseBuffer ();
	reader.closed ();
    }

    @Override public void timeout () {
	// If buffer exists it only holds a partial http header.
	// We relase the buffer and discard that partial header.
	releaseBuffer ();
	reader.timeout ();
    }
    
    public void read () {
	Logger logger = getLogger ();
	logger.finest ("HttpHeaderReader reading data");
	try {
	    // read http request
	    // make sure we have room for reading.
	    ByteBuffer buffer = getBuffer ();
	    int pos = buffer.position ();
	    buffer.limit (buffer.capacity ());
	    int read = getChannel ().read (buffer);
	    if (read == -1) {
		buffer.position (buffer.limit ());
		closeDown ();
		reader.closed ();
		return;
	    } 
	    if (read == 0) {
		closeDown ();
		reader.failed (new IOException ("read 0 bytes, shutting " + 
						"down connection"));
		return;
	    }
	    tl.read (read);
	    buffer.position (startParseAt);
	    buffer.limit (read + pos);
	    parseBuffer (buffer);
	} catch (BadHttpHeaderException e) {
	    closeDown ();
	    reader.failed (e);
	} catch (IOException e) {
	    closeDown ();
	    reader.failed (e);
	}
    }

    private void parseBuffer (ByteBuffer buffer) throws IOException {
	buffer.mark ();
	boolean done = headerParser.handleBuffer (buffer);
	Logger logger = getLogger ();
	if (logger.isLoggable (Level.FINEST))
	    logger.finest ("HttpHeaderReader.parseBuffer: done " + done);
	if (!done) {
	    int pos = buffer.position ();
	    buffer.reset ();
	    if (buffer.position () > 0) {
		// ok, some data handled, make space for more.
		buffer.compact ();
		startParseAt = 0;
	    } else {
		// ok, we did not make any progress, did we only read
		// a partial long line (cookie or whatever).
		if (buffer.limit () < buffer.capacity ()) {
		    // try to read some more
			buffer.position (pos); // Redeemer: |MP~~~~~~L____C|, pos should be equal to limit
		} else  if (isUsingSmallBuffer (buffer)) {
		    // try to expand buffer
		    buffer = getLargeBuffer ();
		    buffer.position (pos); // Redeemer: |MP~~~~~~~~~~LC|, pos should be equal to limit
		    //startParseAt = 0; // Redeemer: buffer.position() is 0 what means startParseAt has to be 0 too
		} else {
		    releaseBuffer ();
		    // ok, we did no progress, abort, client is sending
		    // too long lines.
		    throw new RequestLineTooLongException ();
		}
	    }
	    waitForRead (this);
	} else {
	    HttpHeader header = headerParser.getHeader ();
	    ConnectionSetupResolver resolver = new ConnectionSetupResolver(header);
	    releaseBuffer ();
	    reader.httpHeaderRead (header, getBufferHandle (), 
				   resolver.isKeepalive(), resolver.isChunked(), resolver.getDataSize());
	}
    }
}
