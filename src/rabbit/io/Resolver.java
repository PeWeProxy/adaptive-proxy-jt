package rabbit.io;

import java.net.URL;

/** An interface to handle name lookups. 
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public interface Resolver {
    /** Get the InetAddress for a given url. 
     *  Normally the InetAddress of the url host, but 
     *  might be the InetAddress of the chained proxy to use.
     * @param url the URL to lookup.
     * @param listener the InetAddressListener to notify when lookup is done.
     */
    void getInetAddress (URL url, InetAddressListener listener);
    
    /** Get the port to use for connecting to a given port.
     *  Normally port is returned, but if there is a chained proxy, 
     *  then the proxy port is returned instead.
     */
    int getConnectPort (int port); 

    /** Check if the resolver is using a proxy or not. 
     */
    boolean isProxyConnected ();

    /** Get the currently set proxy authentication. 
     */
    String getProxyAuthString ();
}

