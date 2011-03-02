package sk.fiit.peweproxy.plugins.services.impl.content;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

public interface ServicesContentSource {
	byte[] getData();
	
	void setData(byte[] data);
	
	void ceaseData(byte[] data);
	
	Charset getCharset() throws UnsupportedCharsetException, IOException ;
}
