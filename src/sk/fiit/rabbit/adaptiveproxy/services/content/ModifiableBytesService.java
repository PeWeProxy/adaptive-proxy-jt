package sk.fiit.rabbit.adaptiveproxy.services.content;

public interface ModifiableBytesService extends ByteContentService {
	void setData(byte[] data);
}
