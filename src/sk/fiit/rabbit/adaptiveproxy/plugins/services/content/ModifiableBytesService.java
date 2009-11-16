package sk.fiit.rabbit.adaptiveproxy.plugins.services.content;

public interface ModifiableBytesService extends ByteContentService {
	void setData(byte[] data);
}
