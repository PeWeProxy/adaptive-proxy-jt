package rabbit.util;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Locale;

import rabbit.http.HttpHeader;

public final class HeaderUtils {
	private static final NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
	private static final String TE_CHUNKED = "chunked";
	
	private HeaderUtils() {}
	
	public static void removeContentHeaders(HttpHeader header) {
		header.removeHeader("Transfer-Encoding");
		header.removeHeader("Trailer");
		header.removeHeader("Content-Encoding");
		header.removeHeader("Content-Language");
		header.removeHeader("Content-Length");
		header.removeHeader("Content-Location");
		header.removeHeader("Content-MD5");
		header.removeHeader("Content-Range");
		header.removeHeader("Content-Type");
	}
	
	public static void removeCacheHeaders(HttpHeader header) {
		header.removeHeader("Pragma");
		header.removeHeader("Cache-Control");
		header.removeHeader("ETag");
		// not all cache-related fields are here, just those we are >sure< will be in the header
	}
	
	public static boolean doesClientAcceptGzip(HttpHeader header) {
    	/*
    	 * Accept-Encoding: compress, gzip
    	 * Accept-Encoding:
    	 * Accept-Encoding: *
    	 * Accept-Encoding: compress;q=0.5, gzip;q=1.0
    	 * Accept-Encoding: gzip;q=1.0, identity; q=0.5, *;q=0
    	 */
    	boolean undirectAccept = false;
    	Iterator<String> iter = header.getHeaders("Accept-Encoding").iterator();
    	if (!iter.hasNext()) {
    		/* RFC 2616:
    		 * "If no Accept-Encoding field is present in a request, the server MAY
    		 * assume that the client will accept any content coding."
    		 */
			//return true;
    		
    		/* FireFox corrupts responses when it's Accept-Encoding config value is cleared
    		 * and it does not include any Accept-Encoding field in request
    		 */
    		return false;
		}
    	while(iter.hasNext()) {
    		String prefs = iter.next();
    		Boolean gzipAccept = isEncAccepted(prefs, "gzip");
    		if (gzipAccept != null)
    			return gzipAccept.booleanValue();
    		Boolean otherAccept = isEncAccepted(prefs, "*");
    		if (otherAccept != null) {
    			undirectAccept = !otherAccept;
    		}
    	}
    	return undirectAccept;
    }
    
    private static Boolean isEncAccepted(String prefs, String enc) {
    	int index = -1;
    	if ((index = prefs.indexOf(enc)) >= 0) {
			index += enc.length();
			String following = prefs.substring(index).trim();
			if (following.length() == 0)
				return true;
			char nextChar = following.charAt(0);
	    	if (nextChar == ',')
	    		return true;
	    	if (nextChar == ';') {
	    		// cut ;q=
	    		following = following.substring(3);
	    		int end = following.indexOf(',');
	    		if (end < 0)
	    			end = following.length();
	    		following = following.substring(0, end);
	    		try {
	    			return nf.parse(following).doubleValue() > 0;
				} catch (ParseException ignored) {}
			}
    	}
    	return null;
    }
    
    public static void removeChunkedEncoding(HttpHeader header) {
    	String teHeader = header.getHeader("Transfer-Encoding");
    	if (teHeader.toLowerCase().endsWith(TE_CHUNKED)) {
    		teHeader = teHeader.substring(0, teHeader.length()-TE_CHUNKED.length()).trim();
    		if (teHeader.charAt(teHeader.length()) == ',')
    			teHeader = teHeader.substring(0, teHeader.length()-1);
    	}
    	if (teHeader.trim().isEmpty())
    		header.removeHeader("Transfer-Encoding");
    	else
    		header.setHeader("Transfer-Encoding",teHeader);
    }
}
