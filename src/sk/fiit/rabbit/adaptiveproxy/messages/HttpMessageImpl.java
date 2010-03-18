package sk.fiit.rabbit.adaptiveproxy.messages;

public abstract class HttpMessageImpl {
	byte[] data = null;
	
	public void setData(byte[] data) {
		this.data = data;
	}
	
	public byte[] getData() {
		return data;
	}
}
