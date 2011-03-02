package sk.fiit.peweproxy.plugins.services.impl.content;

public interface ServicesContentSource {
	byte[] getData();
	
	void setData(byte[] data);
	
	void ceaseData(byte[] data);
}
