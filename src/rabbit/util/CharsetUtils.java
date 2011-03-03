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
import java.nio.charset.CoderResult;
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
			if (data.length != 0) {
				charset = cpDetector.detectCodepage(inStream, data.length);
				if (charset instanceof UnknownCharset && useJChardet) {
					inStream = new ByteArrayInputStream(data);
					charset = cpDetector_JChardet.detectCodepage(inStream, data.length);
				}
			}
		}
		if (charset == null || charset instanceof UnknownCharset)
			charset = defaultCharset;
		return charset;
	}
	
	/**
	 * Includes only slightly modified {@link CharsetDecoder#decode(ByteBuffer)} code.
	 */
	public static CharBuffer decodeBytes(ByteBuffer buffer, Charset charset) throws CharacterCodingException, IOException {
		if (buffer == null || !buffer.hasRemaining())
			return CharBuffer.wrap("");
		CharsetDecoder decoder = charset.newDecoder();
		decoder.onMalformedInput(CodingErrorAction.REPORT);
		decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
		
		/*boolean debug = false;
		ByteBuffer origIn = buffer;
		if (debug) {
			buffer = ByteBuffer.allocate(origIn.remaining()+2);
			buffer.put((byte)0xE2);
			buffer.put((byte)0x82);
			buffer.put(origIn);
			buffer.rewind();
			debug = false;
		}
		if (debug) {
			buffer = ByteBuffer.allocate(origIn.remaining()+1);
			buffer.put(origIn);
			buffer.put((byte)0xAC);
			buffer.rewind();
		}*/
		
		// START of copy-paste of CharsetDecoder.decode(ByteBuffer) method code
		// with little modifications
		
		int n = (int)(buffer.remaining() * decoder.averageCharsPerByte());
		CharBuffer out = CharBuffer.allocate(n);

		if ((n == 0) && (buffer.remaining() == 0))
		    return out;
		decoder.reset();
		for (;;) {
		    CoderResult cr = buffer.hasRemaining() ?
			decoder.decode(buffer, out, true) : CoderResult.UNDERFLOW;
		    if (cr.isUnderflow())
			cr = decoder.flush(out);

		    if (cr.isUnderflow())
			break;
		    if (cr.isOverflow()) {
			n = 2*n + 1;	// Ensure progress; n might be 0!
			CharBuffer o = CharBuffer.allocate(n);
			out.flip();
			o.put(out);
			out = o;
			continue;
		    }
		    if (cr.isMalformed())
		    	break; // return what is decoded, leaving some bytes remaining in input buffer
		    else
		    	cr.throwException(); // should not happen since we're REPLACEing unmappable characters
		}
		out.flip();
		
		/*if (buffer != origIn) {
			int newPos = origIn.position()-buffer.remaining();
			origIn.position(newPos);
			origIn.put(buffer);
			origIn.position(newPos);
		}*/
		return out;
	}
}
