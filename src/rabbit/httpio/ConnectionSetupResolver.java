package rabbit.httpio;

import rabbit.http.HttpHeader;

public class ConnectionSetupResolver {
	private boolean keepalive = true;
    private boolean ischunked = false;
    private long dataSize = -1;   // -1 for unknown.
    
	public ConnectionSetupResolver(HttpHeader header) {
		dataSize = -1;
		String cl = header.getHeader ("Content-Length");
		if (cl != null) {
		    try {
			dataSize = Long.parseLong (cl);
		    } catch (NumberFormatException e) {
			dataSize = -1;
		    }
		}
		String con = header.getHeader ("Connection");
		// Netscape specific header...
		String pcon = header.getHeader ("Proxy-Connection");
		if (con != null && con.equalsIgnoreCase ("close"))
		    setKeepAlive (false);
		if (keepalive && pcon != null && pcon.equalsIgnoreCase ("close"))
		    setKeepAlive (false);
		String httpVersion = header.getHTTPVersion ();
		if (httpVersion != null) {
			if (header.isResponse ()) {
			    if (httpVersion.equals ("HTTP/1.1")) {
					String chunked = header.getHeader ("Transfer-Encoding");
					setKeepAlive (true);
					ischunked = false;
					
					if (chunked != null && chunked.toLowerCase().endsWith ("chunked")) {
					    /* If we handle chunked data we must read the whole page
					     * before continuing, since the chunk footer must be 
					     * appended to the header (read the RFC)...
					     * 
					     * As of RFC 2616 this is not true anymore...
					     * this means that we throw away footers and it is legal.
					     */
					    ischunked = true;
					    header.removeHeader ("Content-Length");
					    dataSize = -1;
					}
			    } else {
			    	setKeepAlive (false);
			    }
			    if (!(dataSize > -1 || ischunked))
				setKeepAlive (false);
			} else {
				if (httpVersion.equals ("HTTP/1.1")) {
				    String chunked = header.getHeader ("Transfer-Encoding");
				    if (chunked != null && chunked.toLowerCase().endsWith ("chunked")) {
						ischunked = true;
						header.removeHeader ("Content-Length");
						dataSize = -1;
				    }
				} else if (httpVersion.equals ("HTTP/1.0")) {
				    String ka = header.getHeader ("Connection");
				    if (ka == null || !ka.equalsIgnoreCase ("Keep-Alive"))
					setKeepAlive (false);			
				}
			}
		}
	}
	
	public static boolean isChunked(HttpHeader header) {
		if ("HTTP/1.1".equals(header.getHTTPVersion())) {
			String teVal = header.getHeader ("Transfer-Encoding");
			if (teVal != null && teVal.toLowerCase().endsWith("chunked"))
				return true;
	    }
		return false;
	}
	
	public static boolean chunkingPossible(HttpHeader header) {
		return "HTTP/1.1".equals(header.getHTTPVersion());
	}
	
	/*public static boolean isDynamicLength(HttpHeader header) {
		if (isChunked(header))
			return true;
		if ("HTTP/1.1".equals(header.getResponseHTTPVersion())) {
			String teVal = header.getHeader ("Transfer-Encoding");
			teVal = header.getHeader ("Connection");
			if (teVal != null && teVal.equals("close"))
				return true;
			// otherwise the resource MAY terminate connection, but we can NOT count on this
			// because it probably won't
	    }
		return false;
	}*/
	
	/** Set the keep alive value to currentkeepalive & keepalive
     * @param keepalive the new keepalive value.
     */
    private void setKeepAlive (boolean keepalive) {
	this.keepalive = (this.keepalive && keepalive);
    }

	public boolean isKeepalive() {
		return keepalive;
	}

	public boolean isChunked() {
		return ischunked;
	}

	public long getDataSize() {
		return dataSize;
	}
}
