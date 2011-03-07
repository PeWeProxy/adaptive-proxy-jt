package sk.fiit.peweproxy.messages;

import java.util.Map;

/**
 * Custom data storage that plugins can use to store any kind of data for the whole lifetime of the
 * particular HTTP message processed in AdaptiveProxy platform. Processing storage is basicly a {@link Map}
 * with keys and values of type {@link Object}, with methods parameterized by calling code types for
 * convenience.
 * @author <a href="mailto:redeemer.sk@gmail.com">Jozef Tomek</a>
 */
public interface ProcessingStorage {
	/**
	 * Returns mapping for passed <i>key</i>. See {@link Map#get(Object)}.
	 * @param <T> desired type of the value
	 * @param key the key whose associated value is to be returned 
	 * @return the value to which the specified key is mapped, or <code>null</code> if this
	 * map contains no mapping for the key
	 */
	public <T> T getValue(Object key);
	
	/**
	 * Removes and returns mapping for passed <i>key</i>. See {@link Map#remove(Object)}.
	 * @param <T> desired type of the value
	 * @param key the key whose mapping is to be removed from the map
	 * @return the previous value associated with key, or <code>null</code> if there was no mapping for key
	 */
	public <T> T removeValue(Object key);
	
	/**
	 * Sets mapping for passed <i>key</i>. See {@link Map#put(Object, Object)}.
	 * @param <T> type of the value to be stored
	 * @param key the key whose associated value is to be stored
	 * @param value the value to be associated with the specified key
	 */
	public <T> void setValue(Object key, T value);
}
