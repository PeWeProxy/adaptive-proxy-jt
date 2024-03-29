package rabbit.filter;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.InetAddress;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import rabbit.filter.authenticate.AuthUserInfo;
import rabbit.filter.authenticate.Authenticator;
import rabbit.filter.authenticate.PlainFileAuthenticator;
import rabbit.filter.authenticate.SQLAuthenticator;
import rabbit.http.HttpHeader;
import rabbit.proxy.Connection;
import rabbit.proxy.HttpGenerator;
import rabbit.util.SProperties;

/** This is a filter that requires users to use proxy-authentication.
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public class ProxyAuth implements HttpFilter {
    private final Logger logger = Logger.getLogger (getClass ().getName ());
    private Authenticator authenticator;
    private int cacheTime;
    private boolean oneIpOnly;
    private Pattern noAuthPattern;

    /** Username to user info */
    private final Map<String, AuthUserInfo> cache =
	new ConcurrentHashMap<String, AuthUserInfo> ();

    /** test if a socket/header combination is valid or return a new HttpHeader.
     *  Check that the user has been authenticate..
     * @param socket the SocketChannel that made the request.
     * @param header the actual request made.
     * @param con the Connection handling the request.
     * @return null if everything is fine or a HttpHeader
     *         describing the error (like a 403).
     */
    public HttpHeader doHttpInFiltering (SocketChannel socket,
					 HttpHeader header, Connection con) {
	if (con.getMeta ())
	    return null;
	if (noAuthRequired (header))
	    return null;
	String username = con.getUserName ();
	String token = authenticator.getToken (header, con);
	if (username == null || token == null)
	    return getError (header, con);
	SocketChannel channel = con.getChannel ();
	AuthUserInfo ce = cache.get (username);
	if (hasValidCache (token, ce)) {
	    if (oneIpOnly) {
		InetAddress ia = channel.socket ().getInetAddress ();
		if (!ce.correctSocketAddress (ia)) 
		    return getError (header, con);
	    }
	    return null;
	}

	if (!authenticator.authenticate (username, token))
	    return getError (header, con);
	if (cacheTime > 0)
	    storeInCache (username, token, channel);
	return null;
    }

    private boolean noAuthRequired (HttpHeader header) {
	if (noAuthPattern == null)
	    return false;
	return noAuthPattern.matcher (header.getRequestURI ()).find ();
    }

    private boolean hasValidCache (String token, AuthUserInfo ce) {
	if (ce == null)
	    return false;
	if (!ce.stillValid ())
	    return false;
	if (!ce.correctToken (token))
	    return false;
	return true;
    }

    private void storeInCache (String user, String token,
			       SocketChannel channel) {
	long timeout =
	    System.currentTimeMillis () + 60000L * cacheTime;
	InetAddress sa =
	    channel.socket ().getInetAddress ();
	AuthUserInfo ce = new AuthUserInfo (token, timeout, sa);
	cache.put (user, ce);
    }

    private HttpHeader getError (HttpHeader header, Connection con) {
	HttpGenerator hg = con.getHttpGenerator ();
	try {
	    return hg.get407 ("internet", new URL (header.getRequestURI ()));
	} catch (MalformedURLException e) {
	    logger.log (Level.WARNING, "Bad url: " + header.getRequestURI (), e);
	    return hg.get407 ("internet", null);
	}
    }

    /** test if a socket/header combination is valid or return a new HttpHeader.
     *  does nothing.
     * @param socket the SocketChannel that made the request.
     * @param header the actual request made.
     * @param con the Connection handling the request.
     * @return This method always returns null.
     */
    public HttpHeader doHttpOutFiltering (SocketChannel socket,
					  HttpHeader header, Connection con) {
	return null;
    }

    /** Setup this class with the given properties.
     * @param properties the new configuration of this class.
     */
    public void setup (SProperties properties) {
	String ct = properties.getProperty ("cachetime", "0");
	cacheTime = Integer.parseInt (ct);
	String ra = properties.getProperty ("one_ip_only", "true");
	oneIpOnly = Boolean.valueOf (ra);
	String allow = properties.getProperty ("allow_without_auth");
	if (allow != null) 
	    noAuthPattern = Pattern.compile (allow);
	String authType = properties.getProperty ("authenticator", "plain");
	if ("plain".equalsIgnoreCase (authType)) {
	    authenticator = new PlainFileAuthenticator (properties);
	} else if ("sql".equalsIgnoreCase (authType)) {
	    authenticator = new SQLAuthenticator (properties);
	} else {
	    try {
		Class<? extends Authenticator> clz =
		    Class.forName (authType).asSubclass (Authenticator.class);
		authenticator = clz.newInstance ();
	    } catch (ClassNotFoundException e) {
		logger.warning ("Failed to find class: '" + authType + "'");
	    } catch (InstantiationException e) {
		logger.warning ("Failed to instantiate: '" + authType + "'");
	    } catch (IllegalAccessException e) {
		logger.warning ("Failed to instantiate: '" + authType +
				"': " + e);
	    }
	}
    }
}
