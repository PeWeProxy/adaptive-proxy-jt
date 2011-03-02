package sk.fiit.peweproxy.plugins.services.impl.content;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import rabbit.util.CharsetUtils;
import sk.fiit.peweproxy.headers.HeaderWrapper;
import sk.fiit.peweproxy.headers.ReadableHeader;
import sk.fiit.peweproxy.headers.WritableHeader;
import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.services.ServicesHandle;
import sk.fiit.peweproxy.services.content.ModifiableStringService;

public class ModifiableStringServiceImpl extends BaseStringServicesProvider<ModifiableStringService>
 implements ModifiableStringService{
	
	public ModifiableStringServiceImpl(HeaderWrapper actualHeader, boolean useJChardet, ServicesContentSource content)
		throws CharacterCodingException, UnsupportedCharsetException, IOException {
		super(actualHeader, content, useJChardet);
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
		if (charset == null)
			throw new IllegalArgumentException("charset can not be null");
		// no check whether changing charset is valid ( = header not already sent)
		// to allow receiving chunk in original charset and sending it in new one
		// (changing already sent header appropriately before it is sent is the
		// responsibility of the plugin itself)
	}

	@Override
	public void setContent(String content) {
		sb.setLength(0);
		if (content != null)
			sb.append(content);
	}
	
	void doChanges(ReadableHeader origHeader, WritableHeader targetHeader) {
		String s = sb.toString();
		content.setData(s.getBytes(charset));
		if (origHeader == null)
			// service over chunk, can't modify header
			return;
		if (origHeader.getField("Content-Length") != null) {
			targetHeader.setField("Content-Length", Integer.toString(content.getData().length, 10));
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

	@Override
	public void doChanges(HttpRequest request,
			ServicesHandle chunkServicesHandle) {
		doChanges((ReadableHeader)null, (WritableHeader)null);
	}

	@Override
	public void doChanges(HttpResponse response,
			ServicesHandle chunkServicesHandle) {
		doChanges((ReadableHeader)null, (WritableHeader)null);
	}
}
