package rabbit.httpio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.WritableByteChannel;
import rabbit.io.BufferHandle;
import rabbit.io.ConnectionHandler;
import rabbit.io.SelectorRegistrator;
import rabbit.io.SocketHandler;
import rabbit.io.WebConnection;
import rabbit.util.Logger;
import rabbit.util.TrafficLogger;

/** A resource source that gets the data from a WebConnection
 * 
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public class WebConnectionResourceSource 
    implements ResourceSource, SocketHandler, ChunkDataFeeder {
    private final ConnectionHandler con;
    private final Selector selector;
    private final WebConnection wc;
    private final BufferHandle bufHandle;
    private final Logger logger;
    private final TrafficLogger tl;
    private BlockListener listener;
    private final boolean isChunked;
    private final long dataSize;
    private long totalRead = 0;    
    private int currentMark = 0;
    private ChunkHandler chunkHandler;
    
    public WebConnectionResourceSource (ConnectionHandler con, 
					Selector selector, WebConnection wc, 
					BufferHandle bufHandle, Logger logger,
					TrafficLogger tl, boolean isChunked, 
					long dataSize, boolean strictHttp) {
	this.con = con;
	this.selector = selector;
	this.wc = wc;
	this.bufHandle = bufHandle;
	this.logger = logger;
	this.tl = tl;
	this.isChunked = isChunked;
	if (isChunked)
	    chunkHandler = new ChunkHandler (this, strictHttp);
	this.dataSize = dataSize;
    }

    public String getDescription () {
	return "WebConnectionResourceSource: length: "+ dataSize + 
	    ", read: " + totalRead + ", chunked: " + isChunked + 
	    ", address: " + wc.getAddress ();
    }

    /** FileChannels can not be used, will always return false.
     * @return false
     */
    public boolean supportsTransfer () {
	return false;
    }

    public long length () {
	return dataSize;
    }

    public long transferTo (long position, long count, 
			    WritableByteChannel target)
	throws IOException {
	throw new IllegalStateException ("transferTo can not be used.");
    }
    
    public void addBlockListener (BlockListener listener) {
	this.listener = listener;
	if (isChunked)
	    chunkHandler.addBlockListener (listener);
	if (dataSize > -1 && totalRead >= dataSize) {
	    cleanupAndFinish ();
	} else if (bufHandle.isEmpty ()) {
	    register ();
	} else {       
	    handleBlock ();
	}
    }

    public void finishedRead () {
	cleanupAndFinish ();
    }

    private void cleanupAndFinish () {
	listener.finishedRead ();
    }

    public void register () {
	try {
	    SelectorRegistrator.register (logger, wc.getChannel (), 
					  selector, SelectionKey.OP_READ, 
					  this);
	} catch (IOException e) {
	    listener.failed (e);
	}
    }

    private void handleBlock () {
	if (isChunked) {
	    chunkHandler.handleData (bufHandle);
	    totalRead = chunkHandler.getTotalRead ();
	} else {
	    ByteBuffer buffer = bufHandle.getBuffer ();
	    totalRead += buffer.remaining ();		
	    listener.bufferRead (bufHandle);
	}
	bufHandle.possiblyFlush ();
    }

    public void readMore () {
	if (!bufHandle.isEmpty ()) {
	    ByteBuffer buffer = bufHandle.getBuffer ();
	    buffer.compact ();
	    currentMark = buffer.position ();
	}
	register ();
    }

    public void run () {
	ByteBuffer buffer = bufHandle.getBuffer ();
	buffer.position (currentMark); // keep our saved data.
	int limit = buffer.capacity ();
	if (dataSize > 0  && !isChunked) {
	    limit = currentMark + (int)Math.min (limit - currentMark, 
						 dataSize - totalRead);
	}
	buffer.limit (limit);
	try {
	    int read = wc.getChannel ().read (buffer);
	    currentMark = 0;
	    if (read == 0) {
		bufHandle.possiblyFlush ();
		register ();
	    } else if (read == -1) {
		bufHandle.possiblyFlush ();
		cleanupAndFinish ();
	    } else {
		tl.read (read);
		buffer.flip ();
		handleBlock ();
	    }
	} catch (IOException e) {
	    listener.failed (e);
	}
    }

    public boolean useSeparateThread () {
	return false;
    }

    public void timeout () {
	listener.timeout ();
    }

    public void release () {
	if (!bufHandle.isEmpty () && wc.getKeepalive () &&
	    (dataSize < 0 || totalRead != dataSize))
	    wc.setKeepalive (false);
	if (!wc.getKeepalive () && !bufHandle.isEmpty ()) {
	    // empty the buffer so we can reuse it.
	    ByteBuffer buffer = bufHandle.getBuffer ();
	    buffer.position (buffer.limit ());
	}
	    
	bufHandle.possiblyFlush ();
	con.releaseConnection (wc);
    }
}
