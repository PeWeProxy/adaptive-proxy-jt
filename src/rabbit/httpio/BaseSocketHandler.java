package rabbit.httpio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import rabbit.io.BufferHandle;
import rabbit.io.SelectorRegistrator;
import rabbit.io.SocketHandler;
import rabbit.util.Logger;

/** A base class for socket handlers.
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public abstract class BaseSocketHandler implements SocketHandler {
    /** The client channel. */
    protected SocketChannel channel; 
    
    /** The selector we are using. */
    protected Selector selector;

    /** The selection key we are using. */
    protected SelectionKey sk;

    /** The logger to use. */
    protected Logger logger;
    
    /** The buffer handle. */
    protected BufferHandle bh;
    
    public BaseSocketHandler (SocketChannel channel, BufferHandle bh, 
			      Selector selector, Logger logger) 
	throws IOException {
	this.channel = channel;
	this.bh = bh;
	this.selector = selector;
	this.logger = logger;
	register ();
    }

    protected void register () throws ClosedChannelException {
	int ops = getSocketOperations ();
	if (ops != 0)
	    sk = SelectorRegistrator.register (logger, channel, 
					       selector, ops, this);
    }
    
    protected ByteBuffer getBuffer () {
	return bh.getBuffer ();
    }

    protected void growBuffer () {
	bh.growBuffer ();
    }

    protected void releaseBuffer () {
	bh.possiblyFlush ();
    }

    protected abstract int getSocketOperations ();
    
    public boolean useSeparateThread () {
	return false;
    }

    protected Logger getLogger () {
	return 	logger;
    }

    protected void closeDown () {
	try {
	    releaseBuffer ();
	    sk.attach ("BaseSocketHandler.closeDown");
	    sk.cancel ();
	    channel.close ();
	    clear ();
	} catch (IOException e) {
	    getLogger ().logWarn ("Failed to close down connection: " + e);
	}	
    }

    protected void unregister () {
	clear ();
    }
    
    private void clear () {
	sk = null;
	logger = null;
	selector = null;
	channel = null;
    }
}
