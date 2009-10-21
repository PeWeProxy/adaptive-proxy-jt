package rabbit.httpio;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/** A listener for accepted connections.
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public interface AcceptorListener {
    /** A conneciton has been accepted 
     * @param sc the new socket channel, will already be set to non blocking mode
     * @param selector the selector that accepted the connection
     */
    void connectionAccepted (SocketChannel sc, Selector selector) 
	throws IOException;
}
