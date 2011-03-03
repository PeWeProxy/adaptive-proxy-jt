package sk.fiit.peweproxy.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ChunksRemainsImpl implements ChunkRemains {
	final Map<Object, Object> remains = new HashMap<Object, Object>();
	byte[] ceasedData = null;
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getRemains(Object key) {
		return (T)remains.get(key);
	}

	@Override
	public <T> void setRemains(Object key, T remains) {
		this.remains.put(key, remains);
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
