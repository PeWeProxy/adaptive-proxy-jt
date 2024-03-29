package rabbit.dns;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import rabbit.util.SProperties;

/** A DNS handler. 
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public interface DNSHandler {
    /** Do any neccessary setup. 
     * @param config the properties for this handler
     */
    void setup (SProperties config);
    
    /** Look up an internet address. 
     * @param url the url to get the host from
     */
    InetAddress getInetAddress (URL url) 
	throws UnknownHostException;    

    /** Look up an internet address. 
     * @param host the name of the host to lookup
     */
    InetAddress getInetAddress (String host) 
	throws UnknownHostException;
}
