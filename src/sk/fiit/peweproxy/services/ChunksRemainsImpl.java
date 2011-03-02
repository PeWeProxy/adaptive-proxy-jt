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
		else {
			byte[] tmp = ceasedData;
			// make large array and copy data to the beginning
			ceasedData = Arrays.copyOf(data, data.length+ceasedData.length);
			// copy original ceasedData to the end of new array
			System.arraycopy(tmp, 0, ceasedData, data.length, tmp.length);
		}
	}
	
	public byte[] joinCeasedData(byte[] chunkData) {
		if (ceasedData == null || ceasedData.length == 0)
			return chunkData;
		byte[] tmp = chunkData;
		chunkData = Arrays.copyOf(ceasedData, ceasedData.length+chunkData.length);
		System.arraycopy(tmp, 0, chunkData, ceasedData.length, tmp.length);
		return chunkData;
	}
}
