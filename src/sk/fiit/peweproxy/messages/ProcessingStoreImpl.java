package sk.fiit.peweproxy.messages;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class ProcessingStoreImpl implements ProcessingStorage {
	Map<Object, Object> values = new HashMap<Object, Object>();
	byte[] ceasedData = null;
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getValue(Object key) {
		return (T)values.get(key);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T removeValue(Object key) {
		return (T)values.remove(key);
	}

	@Override
	public <T> void setValue(Object key, T value) {
		this.values.put(key, value);
	}
	
	public void copyValues(ProcessingStoreImpl otherStore) {
		this.values = otherStore.values;
	}
	
	public void ceaseData(byte[] data) {
		if (ceasedData == null)
			ceasedData = data;
		if (data == null || data.length == 0)
			return;
		else {
			joinArrays(data, ceasedData);
		}
	}
	
	private byte[] joinArrays(byte[] header, byte[] trailer) {
		byte[] tmp = trailer;
		// make large array and copy header to the beginning
		trailer = Arrays.copyOf(header, header.length+trailer.length);
		// append original trailer to the end of new array
		System.arraycopy(tmp, 0, trailer, header.length, tmp.length);
		return trailer;
	}
	
	public byte[] joinCeasedData(byte[] chunkData) {
		if (ceasedData == null || ceasedData.length == 0)
			return chunkData;
		if (chunkData == null || chunkData.length == 0)
			return ceasedData;
		byte[] ceasedData = this.ceasedData;
		this.ceasedData = null;
		return joinArrays(ceasedData, chunkData);
	}
}
