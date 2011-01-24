package sk.fiit.peweproxy.plugins.services.impl.content;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import rabbit.util.CharsetUtils;
import sk.fiit.peweproxy.headers.ReadableHeader;
import sk.fiit.peweproxy.headers.WritableHeader;
import sk.fiit.peweproxy.messages.HttpMessageImpl;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.services.content.ModifiableStringService;

public class ModifiableStringServiceImpl extends BaseMessageServiceProvider<ModifiableStringService>
	implements ModifiableStringService{

	private final StringBuilder sb;
	private Charset charset = null;
	
	public ModifiableStringServiceImpl(HttpMessageImpl<?> httpMessage, boolean useJChardet)
		throws CharacterCodingException, UnsupportedCharsetException, IOException {
		super(httpMessage);
		byte[] data = httpMessage.getData();
		charset = CharsetUtils.detectCharset(httpMessage.getHeader(), data, useJChardet);
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
	
	void doChanges(ReadableHeader origHeader, WritableHeader targetHeader) {
		String s = sb.toString();
		httpMessage.setData(s.getBytes(charset));
		if (origHeader.getField("Content-Length") != null) {
			targetHeader.setField("Content-Length", Integer.toString(httpMessage.getData().length, 10));
		}
		String cType = targetHeader.getField("Content-Type");
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
		if (charset != CharsetUtils.defaultCharset) {
			sbTmp.append(leading);
			sbTmp.append(charset.toString());
			sbTmp.append(trailing);
		}
		targetHeader.setField("Content-Type", sbTmp.toString());
	}
	
	/*protected void inspectCharset() {
		Charset newCharset = CharsetUtils.detectCharset(httpMessage.getProxyHeader());
		if (newCharset != null)
			charset = newCharset;
	}*/
	
	@Override
	public void doChanges(ModifiableHttpRequest request) {
		doChanges(request.getOriginalRequest().getRequestHeader(), request.getRequestHeader());
	}

	@Override
	public void doChanges(ModifiableHttpResponse response) {
		doChanges(response.getOriginalResponse().getResponseHeader(), response.getResponseHeader());
	}
}
