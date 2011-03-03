package sk.fiit.peweproxy.plugins.services.impl.content;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import org.apache.log4j.Logger;

import rabbit.util.CharsetUtils;
import sk.fiit.peweproxy.services.ChunkServicesHandle;
import sk.fiit.peweproxy.services.ProxyService;

public abstract class BaseStringServicesProvider<Service extends ProxyService>
 extends BaseContentServiceProvider<String, Service> {
	private static final Logger log = Logger.getLogger(BaseStringServicesProvider.class);
	
	protected final StringBuilder sb;
	protected Charset charset;
	private final boolean fullyDecoded;
	
	public BaseStringServicesProvider(ServicesContentSource content)
	 throws CharacterCodingException, UnsupportedCharsetException, IOException {
		super(content);
		this.charset = content.getCharset();
		byte[] undecodedTrailing = null;
		if (content.getData() == null)
			sb = new StringBuilder("");
		else {
			ByteBuffer buf = ByteBuffer.wrap(content.getData());
			sb = new StringBuilder(CharsetUtils.decodeBytes(buf, charset));
			
			if (buf.hasRemaining()) {
				undecodedTrailing = new byte[buf.remaining()];
				buf.get(undecodedTrailing);
				content.ceaseData(undecodedTrailing);
			}
		}
		fullyDecoded = undecodedTrailing == null;
	}
	
	@Override
	public boolean initChangedModel() {
		return !fullyDecoded;
	}
	
	public final void ceaseContent(String chunkPart, ChunkServicesHandle chunkServiceshandle) {
		if (chunkPart == null || chunkPart.isEmpty())
			return;
		if (!sb.toString().endsWith(chunkPart)) {
			log.debug("Text being ceased is not at the end of current chunk");
		}
		content.ceaseData(chunkPart.getBytes(charset));
	};
}
