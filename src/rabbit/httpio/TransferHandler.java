package rabbit.httpio;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import rabbit.io.Address;
import rabbit.io.SelectorRegistrator;
import rabbit.io.SocketHandler;
import rabbit.util.Logger;
import rabbit.util.TrafficLogger;

/** A handler that transfers data from a Transferable to a socket channel. 
 *  Since file transfers may take time we run in a separate thread.
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public class TransferHandler implements Runnable {
    private final TaskRunner tr;
    private final Transferable t;
    private SelectionKey sk;
    private final Selector selector;
    private final SocketChannel channel;
    private final TrafficLogger tlFrom;
    private final TrafficLogger tlTo;
    private final TransferListener listener;
    private final Logger logger;
    private long pos = 0; 
    private long count;
    
    public TransferHandler (TaskRunner tr, Transferable t, 
			    Selector selector, SocketChannel channel, 
			    TrafficLogger tlFrom, TrafficLogger tlTo, 
			    TransferListener listener, Logger logger) {
	this.tr = tr;
	this.t = t;
	this.selector = selector;
	this.channel = channel;
	this.tlFrom = tlFrom;
	this.tlTo = tlTo;
	this.listener = listener;
	this.logger = logger;
	count = t.length ();
    }

    public void transfer () {
	tr.runThreadTask (this);
    }
    
    public void run () {
	try {
	    while (count > 0) {
		long written = 
		    t.transferTo (pos, count, channel);
		pos += written; 
		count -= written;
		tlFrom.transferFrom (written);
		tlTo.transferTo (written);
		if (count > 0 && written == 0) {
		    setupWaitForWrite ();
		    return;
		}		
	    }
	    returnOk ();
	} catch (IOException e) {
	    returnWithFailure (e);
	}	    
    }

    private void setupWaitForWrite () {
	tr.runMainTask (new WriteWaitRegistrator ());
    }

    private class WriteWaitRegistrator implements Runnable {
	public void run () {
	    try {
		SocketHandler sh = new WriteWaiter ();
		sk = 
		    SelectorRegistrator.register (logger, channel, 
						  selector, 
						  SelectionKey.OP_WRITE,
						  sh, Long.MAX_VALUE);
	    } catch (IOException e) {
		listener.failed (e);
	    }
	}
    }
    
    private class WriteWaiter implements SocketHandler {
	public void run () {
	    if (sk.isValid ()) {
		TransferHandler.this.run ();
	    } else {
		sk.cancel ();
		String err = "write wait got invalid channel";
		returnWithFailure (new IOException (err));
	    }
	}
	
	public void timeout () {
	    if (!sk.isValid ()) {
		sk.cancel ();
	    }
	    returnWithFailure (new IOException ("write timed out"));
	}

	public boolean useSeparateThread () {
	    return true;
	}

	public String getDescription () {
	    Socket s = channel.socket ();
	    Address a = new Address (s.getInetAddress (), s.getPort ());
	    return "TransferHandler$WriteWaiter: address: " + a;
	}
    }
    
    private void returnWithFailure (final Exception cause) {
	tr.runMainTask (new Runnable () {
		public void run () {
		    listener.failed (cause);
		}
	    });	    
    }

    private void returnOk () {
	tr.runMainTask (new TransferOK ());
    }

    private class TransferOK implements Runnable {
	public void run () {
	    listener.transferOk ();
	}
    }
}
