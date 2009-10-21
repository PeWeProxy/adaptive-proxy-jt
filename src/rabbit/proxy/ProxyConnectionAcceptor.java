package rabbit.proxy;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import rabbit.httpio.AcceptorListener;
import rabbit.io.BufferHandler;

/** An acceptor handler that creates proxy client connection
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public class ProxyConnectionAcceptor implements AcceptorListener {
    private int id;
    private long counter;
    private HttpProxy proxy;

    public ProxyConnectionAcceptor (int id, HttpProxy proxy) {
	this.id = id;
	this.proxy = proxy;
    }

    public void connectionAccepted (SocketChannel sc, Selector selector) 
	throws IOException {
	proxy.getCounter ().inc ("Socket accepts");
	if (!proxy.getSocketAccessController ().checkAccess (sc)) {
	    proxy.getLogger ().logWarn ("Rejecting access from " + 
					sc.socket ().getInetAddress ());
	    proxy.getCounter ().inc ("Rejected IP:s");
	    sc.close ();
	} else {
	    BufferHandler bh = proxy.getBufferHandler (selector);
	    Connection c = 
		new Connection (getId (), sc, selector, proxy, bh);
	    c.readRequest ();
	}
    }

    private ConnectionId getId () {
	synchronized (this) {
	    return new ConnectionId (id, counter++);
	}
    }
}
