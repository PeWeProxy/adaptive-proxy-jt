package sk.fiit.rabbit.adaptiveproxy.plugins.services.content;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import rabbit.util.CharsetUtils;
import sk.fiit.rabbit.adaptiveproxy.headers.WritableHeader;
import sk.fiit.rabbit.adaptiveproxy.messages.HttpMessageImpl;
import sk.fiit.rabbit.adaptiveproxy.messages.ModifiableHttpRequest;
import sk.fiit.rabbit.adaptiveproxy.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.services.content.ModifiableStringService;

public class ModifiableStringServiceImpl<MessageType extends HttpMessageImpl<?>>
	extends BaseServiceProvider<HttpMessageImpl<?>,ModifiableStringService> implements ModifiableStringService{

	private final StringBuilder sb;
	private Charset charset = null;
	
	public ModifiableStringServiceImpl(MessageType httpMessage, boolean useJChardet)
		throws CharacterCodingException, UnsupportedCharsetException, IOException {
		super(httpMessage);
		byte[] data = httpMessage.getData();
		charset = CharsetUtils.detectCharset(httpMessage.getProxyHeader(), data, useJChardet);
		sb = new StringBuilder(CharsetUtils.decodeBytes(data, charset, true));
	}

	@Override
	public StringBuilder getModifiableContent() {
		return sb;
	}
	
	@Override
	public Class<ModifiableStringService> getServiceClass() {
		return ModifiableStringService.class;
	}
	
	@Override
	public String getContent() {
		return sb.toString();
	}

	@Override
	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	@Override
	public void setContent(String content) {
		sb.setLength(0);
		sb.append(content);
	}
	
	void doChanges() {
		String s = sb.toString();
		httpMessage.setData(s.getBytes(charset));
		
		WritableHeader proxyHeader = httpMessage.getProxyHeader();
		String cType = proxyHeader.getField("Content-Type");
		if (cType == null)
			return;
		String trailing = "";
		String leading = "charset=";
		int chsIndex = cType.indexOf(leading);
		if (chsIndex != -1) {
			int afterChIndex = cType.substring(chsIndex+8).indexOf(';');
			if (afterChIndex != -1)
				trailing = cType.substring(chsIndex+8+afterChIndex);
		} else {
			chsIndex = cType.length();
			leading = "; "+leading;
		}
		StringBuilder sbTmp = new StringBuilder();
		sbTmp.append(cType.substring(0, chsIndex));
		sbTmp.append(leading);
		sbTmp.append(charset.toString());
		sbTmp.append(trailing);
		proxyHeader.setField("Content-Type", sbTmp.toString());
	}
	
	/*protected void inspectCharset() {
		Charset newCharset = CharsetUtils.detectCharset(httpMessage.getProxyHeader());
		if (newCharset != null)
			charset = newCharset;
	}*/
	
	@Override
	public void doChanges(ModifiableHttpRequest request) {
		doChanges();
	}
	
	@Override
	public void doChanges(ModifiableHttpResponse response) {
		doChanges();
	}
}
