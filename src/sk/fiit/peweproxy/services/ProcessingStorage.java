package sk.fiit.peweproxy.services;

public interface ProcessingStorage {
	public <T> T getValue(Object key);
	
	public <T> T removeValue(Object key);
	
	public <T> void setValue(Object key, T value);
}
