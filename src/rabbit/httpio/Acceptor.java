package rabbit.httpio;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import rabbit.io.SelectorRegistrator;
import rabbit.io.SocketHandler;
import rabbit.util.Logger;

/** A standard acceptor.
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public class Acceptor implements SocketHandler {
    private ServerSocketChannel ssc;
    private SelectorRunner selectorRunner;
    private Logger logger;
    private AcceptorListener listener;

    public Acceptor (ServerSocketChannel ssc, 
		     SelectorRunner selectorRunner,
		     Logger logger,
		     AcceptorListener listener) {
	this.ssc = ssc;
	this.selectorRunner = selectorRunner;
	this.logger = logger;
	this.listener = listener;
    }
	
    public String getDescription () {
	return "Acceptor: ssc: " + ssc;
    }

    /** Acceptor runs in the selector thread.
     */ 
    public boolean useSeparateThread () {
	return false;
    }

    /** Handle timeout, since an acceptor should not get timeouts an 
     *  exception will be thrown.
     */ 
    public void timeout () {
	throw new IllegalStateException ("Acceptor should not get timeout");
    }

    /** Accept a SocketChannel.
     */ 
    public void run () {
	try {
	    SocketChannel sc = ssc.accept ();
	    sc.configureBlocking (false);
	    listener.connectionAccepted (sc, selectorRunner.getSelector ());
	    register ();
	} catch (IOException e) {
	    throw new RuntimeException ("Got some IOException", e);
	}
    }

    /** Register OP_ACCEPT with the selector. 
     */ 
    public void register () throws IOException {
	SelectorRegistrator.register (logger, ssc, 
				      selectorRunner.getSelector (),
				      SelectionKey.OP_ACCEPT, 
				      this, Long.MAX_VALUE);

    }
}
