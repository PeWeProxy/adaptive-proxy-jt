package rabbit.util;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.Map;

import rabbit.http.HttpHeader;

public abstract class CharsetDetector {
	private static final Map<String, String> substitutions;
	
	static {
		substitutions = new HashMap<String, String>();
		substitutions.put("UTF=8", "UTF-8");
	}
	
	public static String detectCharsetString(HttpHeader headers) {
		String cs = null;
		String ct = headers.getHeader("Content-Type");
		if (ct != null) {
		    String look = "charset=";
		    int beginIndex = ct.indexOf(look);
		    if (beginIndex > 0) {
				beginIndex += look.length();
				cs = ct.substring(beginIndex).trim ().replace("\"", "");
				int indexOfSemicolon = cs.indexOf(';');
				if (indexOfSemicolon > -1)
					cs = cs.substring(0, indexOfSemicolon);
				
				/*cs = cs.replace("_", "").replace("-", "");			
				if (cs.equalsIgnoreCase("iso88591"))
					cs = "ISO8859_1";*/
		    }
	    }
		if (cs != null && substitutions.containsKey(cs))
			cs = substitutions.get(cs);
		return cs;
	}
	
	public static Charset detectCharset(HttpHeader headers) throws UnsupportedCharsetException {
		String cs = detectCharsetString(headers);
		if (cs == null)
			return null;
		return Charset.forName(cs);
	}
}
