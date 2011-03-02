package sk.fiit.peweproxy.services;

public interface ChunkRemains {
	public <T> T getRemains(Object key);
	
	public <T> void setRemains(Object key, T remains);
}
