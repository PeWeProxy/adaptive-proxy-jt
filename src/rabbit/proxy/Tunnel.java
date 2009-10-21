package rabbit.proxy;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import rabbit.io.Address;
import rabbit.io.BufferHandle;
import rabbit.io.SelectorRegistrator;
import rabbit.io.SocketHandler;
import rabbit.util.Logger;
import rabbit.util.TrafficLogger;

/** A handler that just tunnels data.
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
class Tunnel implements SocketHandler {
    private Selector selector;
    private Logger logger;
    private SocketChannel from;
    private BufferHandle fromHandle;
    private TrafficLogger fromLogger;
    private SelectionKey fromSk = null;
    private SocketChannel to;
    private BufferHandle toHandle;
    private TrafficLogger toLogger;
    private SelectionKey toSk = null;
    private TunnelDoneListener listener;
    
    public Tunnel (Selector selector, Logger logger, 
		   SocketChannel from, BufferHandle fromHandle,
		   TrafficLogger fromLogger, 
		   SocketChannel to, BufferHandle toHandle, 
		   TrafficLogger toLogger,
		   TunnelDoneListener listener) 
	throws IOException {
	this.selector = selector;
	this.logger = logger;
	this.from = from;
	this.fromHandle = fromHandle;
	this.fromLogger = fromLogger;
	this.to = to;
	this.toHandle = toHandle;
	this.toLogger = toLogger;
	this.listener = listener;
	sendBuffers ();
    }

    public String getDescription () {
	StringBuilder sb = new StringBuilder ("Tunnel: from: ");
	Socket s = from.socket ();
	Address a = new Address (s.getInetAddress (), s.getPort ());
	sb.append (a.toString ()).append (", to: ");
	s = to.socket ();
	a = new Address (s.getInetAddress (), s.getPort ());
	sb.append (a.toString ());
	return sb.toString ();
    }
    
    private boolean isValidForRead (SelectionKey sk) {
	return sk == null ||   // not previously registered
	    (sk.isValid () &&  // previously registered 
	     (sk.interestOps () & SelectionKey.OP_READ) == 0);
    }

    private void registerReadFrom () throws IOException {
	if (listener == null)
	    return;
        toHandle.possiblyFlush ();
        if (isValidForRead (fromSk)) {
            fromSk = SelectorRegistrator.register (logger, from, selector,
						   SelectionKey.OP_READ,
						   this, Long.MAX_VALUE);
        }
    }

    private void registerReadTo () throws IOException {
	if (listener == null)
	    return;
        fromHandle.possiblyFlush ();
        if (isValidForRead (toSk)) {
            toSk = SelectorRegistrator.register (logger, to, selector,
						 SelectionKey.OP_READ,
						 this, Long.MAX_VALUE);
        }
    }

    private void sendBuffers () throws IOException {	
	boolean needMore1 = false;
	needMore1 = sendBuffer (fromHandle, to, toLogger);
	if (needMore1) {
	    toSk = SelectorRegistrator.register (logger, to, selector, 
						 SelectionKey.OP_WRITE, 
						 this, Long.MAX_VALUE);
	}
	
	boolean needMore2 = false;
	needMore2 = sendBuffer (toHandle, from, fromLogger);
	if (needMore2) {
	    fromSk = SelectorRegistrator.register (logger, from, selector, 
						   SelectionKey.OP_WRITE, 
						   this, Long.MAX_VALUE);
	}
	
        if (!needMore1) {
            registerReadTo ();
        }
	
        if (!needMore2) {
            registerReadFrom ();
        }
    }

    /** Send the buffer to the channel. 
     * @return true if more data needs to be written.
     */
    private boolean sendBuffer (BufferHandle bh, SocketChannel channel, 
				TrafficLogger tl) 
	throws IOException {
	if (bh.isEmpty ())
	    return false;
	ByteBuffer buffer = bh.getBuffer ();
	if (buffer.hasRemaining ()) {
	    int written;
	    do {
		written = channel.write (buffer);
		tl.write (written);
	    } while (written > 0 && buffer.remaining () > 0);
	}
	bh.possiblyFlush ();
	return !bh.isEmpty ();
    }
    
    private void readBuffers () throws IOException {
	readBuffer (from, fromHandle, fromLogger);
	readBuffer (to, toHandle, toLogger);
    }
	
    private void readBuffer (SocketChannel channel, BufferHandle bh, 
			    TrafficLogger tl) 
	throws IOException {
	ByteBuffer buffer = bh.getBuffer ();
	int read = channel.read (buffer);
	if (read == -1) {
	    buffer.position (buffer.limit ());
	    closeDown ();
	} 
	buffer.flip ();
	tl.read (read);
    }

    private Logger getLogger () {
	return logger;
    }
	
    public boolean useSeparateThread () {
	return false;
    }

    public void timeout () {
	getLogger ().logWarn ("Tunnel: timeout during handling");
	throw new IllegalStateException ("Tunnels should not get timeout");
    }

    public void run () {
	try {
	    if (fromHandle.isEmpty () && toHandle.isEmpty ()) 
		readBuffers ();
	    sendBuffers ();
	} catch (IOException e) {
	    getLogger ().logWarn ("Tunnel: failed to handle: " + e);
	    closeDown ();
	}
    }

    private void closeDown () {
	fromHandle.possiblyFlush ();
	toHandle.possiblyFlush ();
	// we do not want to close the channels, 
	// it is up to the listener to do that.
	if (listener != null)
	    listener.tunnelClosed ();
	listener = null;
    }
}
