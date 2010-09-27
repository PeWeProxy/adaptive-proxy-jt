package rabbit.util;

import info.monitorenter.cpdetector.io.ASCIIDetector;
import info.monitorenter.cpdetector.io.ByteOrderMarkDetector;
import info.monitorenter.cpdetector.io.CodepageDetectorProxy;
import info.monitorenter.cpdetector.io.JChardetFacade;
import info.monitorenter.cpdetector.io.ParsingDetector;
import info.monitorenter.cpdetector.io.UnknownCharset;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.Map;

import rabbit.http.HttpHeader;
import sk.fiit.peweproxy.headers.HeaderWrapper;
import sk.fiit.peweproxy.headers.ReadableHeader;

public abstract class CharsetUtils {
	public static final Charset defaultCharset = Charset.forName("ISO-8859-1");
	private static final CodepageDetectorProxy cpDetector;
	private static final CodepageDetectorProxy cpDetector_JChardet;
	private static final Map<String, String> substitutions;
	
	static {
		cpDetector = CodepageDetectorProxy.getInstance();
		cpDetector.add(new ByteOrderMarkDetector());
		cpDetector.add(new ParsingDetector(false));
		//cpDetector.add(JChardetFacade.getInstance());
		//cpDetector.add(ASCIIDetector.getInstance());
		cpDetector_JChardet = CodepageDetectorProxy.getInstance();
		cpDetector_JChardet.add(JChardetFacade.getInstance());
		cpDetector_JChardet.add(ASCIIDetector.getInstance());
		substitutions = new HashMap<String, String>();
		substitutions.put("UTF=8", "UTF-8");
	}
	
	public static String detectCharsetString(HttpHeader header) {
		String cs = null;
		String ct = header.getHeader("Content-Type");
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
	
	public static Charset detectCharset(ReadableHeader header) throws UnsupportedCharsetException {
		String cs = detectCharsetString(((HeaderWrapper)header).getBackedHeader());
		if (cs == null)
			return null;
		return Charset.forName(cs);
	}
	
	public static Charset detectCharset(ReadableHeader header, byte[] data, boolean useJChardet) throws UnsupportedCharsetException, IOException {
		Charset charset = detectCharset(header);
		if (charset == null && data != null) {
			InputStream inStream = new ByteArrayInputStream(data);
			charset = cpDetector.detectCodepage(inStream, data.length);
			if (charset instanceof UnknownCharset && useJChardet) {
				inStream = new ByteArrayInputStream(data);
				charset = cpDetector_JChardet.detectCodepage(inStream, data.length);
			}
		}
		if (charset == null || charset instanceof UnknownCharset)
			charset = defaultCharset;
		return charset;
	}
	
	public static CharBuffer decodeBytes(byte[] data, Charset charset, boolean report) throws CharacterCodingException, IOException {
		CodingErrorAction action = (report)? CodingErrorAction.REPORT : CodingErrorAction.REPLACE;
		CharsetDecoder decoder = charset.newDecoder();
		decoder.onMalformedInput(action);
		decoder.onUnmappableCharacter(action);
		return decoder.decode(ByteBuffer.wrap(data));
	}
}
