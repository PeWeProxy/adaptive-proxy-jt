package rabbit.handler;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import rabbit.filter.HtmlFilterFactory;
import rabbit.http.HttpHeader;
import rabbit.httpio.ResourceSource;
import rabbit.io.BufferHandle;
import rabbit.io.SimpleBufferHandle;
import rabbit.proxy.Connection;
import rabbit.proxy.TrafficLoggerHandler;
import sk.fiit.peweproxy.utils.BytesChunker;
import sk.fiit.peweproxy.utils.HeaderUtils;
import sk.fiit.peweproxy.utils.InMemBytesStore;

public class AdaptiveHandler extends FilterHandler {
	private static final org.apache.log4j.Logger log
		= org.apache.log4j.Logger.getLogger(AdaptiveHandler.class);
	
	private boolean transfering = true;
	private boolean sendingPhase = false;
	private InMemBytesStore memStore = null;
	private ByteBuffer buffer = null;
	private Queue<Integer> increments = null;
	private boolean askForCaching = true;
	private boolean doHTMLparsing = false;
	
	public AdaptiveHandler() {}
	
	public AdaptiveHandler(Connection con, TrafficLoggerHandler tlh,
			HttpHeader header, HttpHeader webHeader,
			ResourceSource content, boolean mayCache, boolean mayFilter,
			long size, boolean compress, boolean repack,
			  List<HtmlFilterFactory> filterClasses) {
		super(con, tlh, header, webHeader, content, mayCache,
				mayFilter, size, compress, repack, filterClasses);
		long dataSize = size;
		if (dataSize > 0) {
			if (dataSize > Integer.MAX_VALUE)
				dataSize = Integer.MAX_VALUE;
		} else {
			dataSize = 4096;
		}
		memStore = new InMemBytesStore((int)dataSize);
	}
	
	@Override
	public Handler getNewInstance(Connection con, TrafficLoggerHandler tlh,
			HttpHeader header, HttpHeader webHeader,
			ResourceSource content, boolean mayCache, boolean mayFilter,
			long size) {
		AdaptiveHandler h = new AdaptiveHandler(con, tlh, header, webHeader, content,
				mayCache, mayFilter, size, compress, repack, filterClasses);
		h.askForCaching = askForCaching;
		h.setupHandler();
		if (!askForCaching)
			h.transfering = false;
		askForCaching = true;
		if (log.isDebugEnabled())
			log.debug(h+" is handling "+con+" (requested "
					+header.getRequestLine()+")");
		return h;
	}
	
	private void setHTMLparsing() {
		String ct = response.getHeader("Content-Type");
		if (ct != null && (ct.startsWith("text/html") || ct.startsWith("application/xhtml+xml")))
			doHTMLparsing = true;
		if (log.isDebugEnabled())
			log.debug(this+ " setting"+((doHTMLparsing)? "":" not")+" to parse HTML");
	}
	
	@Override
	public void handle() {
		if (transfering) {
			setHTMLparsing();
			super.handle();
		} else 
			// start receiving content data
			send();
	}
	
	@Override
	protected void handleArray(byte[] arr, int off, int len) {
		if (transfering || sendingPhase) {
			if (doHTMLparsing)
				super.handleArray(arr, off, len);
			else {
				if (!sendingPhase)
					memStore.writeArray(arr, off, len); // for read-only processing
				List<ByteBuffer> buffers = new LinkedList<ByteBuffer>();
				buffers.add(ByteBuffer.wrap(arr, off, len));
				sendBlocks = buffers.iterator();
				if (sendBlocks.hasNext())
					sendBlockBuffers();
				else
					blockSent();
			}
		} else {
			memStore.writeArray(arr, off, len); // for full access processing
			blockSent();
		}
	}

	@Override
	protected void finishData() {
		if (transfering || sendingPhase) {
			Connection con = this.con;
			log.warn(this+" finishData() with con = "+con);
			super.finishData();
			if (!sendingPhase) {
				if (log.isDebugEnabled())
					log.debug(this+" cached data of length "+memStore.getBytes().length + " for read-only processing");
				con.getProxy().getAdaptiveEngine().responseContentCached(con, memStore.getBytes());
			}
		} else {
			if (log.isDebugEnabled())
				log.debug(this+" cached data of length "+memStore.getBytes().length + " for full acccess processing");
			con.getProxy().getAdaptiveEngine().responseContentCached(con, memStore.getBytes(), this);
		}
	}
	
	public void sendResponse(HttpHeader responseHeader, byte[] content) {
		if (sendingPhase) {
			log.warn("sendResponse() method was called second time, closing down connection");
			finish(false);
			return;
		}
		response = responseHeader;
		if (content != null) {
			if (log.isDebugEnabled())
				log.debug(this+" sending response with data of length = "+content.length);
			/*if (con.getChunking() && size > 0) {
					log.debug("Rozdiel je "+(content.length - memStore.getSize()));
					size += content.length - memStore.getSize();
					response.setExistingValue("Content-Length",Long.toString(size, 10));
				}
			}*/
			increments = BytesChunker.adjustBytesIncrements(memStore.getIncrements(), content.length);
			buffer = ByteBuffer.wrap(content);
			if (con.getChunking()) {
				String contentEncoding = response.getHeader("Transfer-Encoding");
				if (log.isDebugEnabled())
					log.debug(this+" contentEncoding: "+contentEncoding);
				if (contentEncoding == null)
					contentEncoding = "";
				if (!contentEncoding.endsWith("chunked"))
					contentEncoding = contentEncoding.concat("chunked");
				response.setHeader("Transfer-Encoding", contentEncoding);
			}
		} else {
			if (log.isDebugEnabled())
				log.debug("Sending response with no data");
			HeaderUtils.removeContentHeaders(response);
			isCompressing = false;
			if (this.content != null) {
				this.content.release();
				this.content = null;
			}
			emptyChunkSent = true;
		}
		gzu = null;
		memStore = null;
		sendingPhase = true;
		writeBytes = true;
		setHTMLparsing();
		sendHeader();
	}
	
	@Override
	protected void send(BufferHandle bufHandle) {
		if (doHTMLparsing && transfering && !sendingPhase) {
			// FilterHandler's methods are messing with HTML blocks
			// 		&& we are not caching for full access processing
			//		&& we are not sending something already cached
			memStore.writeBufferKeepPosition(bufHandle.getBuffer());
		}
		super.send(bufHandle);
	}
	
	@Override
	protected void requestMoreData() {
		if (!sendingPhase)
			super.requestMoreData();
		else
			waitForData();
	}
	
	@Override
	protected void waitForData() {
		if (transfering || !sendingPhase) {
			if (dataRequested) {
				// data already requested (by passing MainBlockListener to SelectorRunner)
				// once in this data-processing cycle, so we ignore the request and clear
				// the flag
				log.debug("Ignoring waitForData() call because data was already requested");
			} else
				super.waitForData();
		} else {
			if (buffer == null)
				finishData();
			else {
				Integer increment = increments.poll();
				if (increment != null) {
					byte[] arr = new byte[increment.intValue()];
					buffer.get(arr);
					BufferHandle bufHandle = new SimpleBufferHandle(ByteBuffer.wrap(arr));
					super.bufferRead(bufHandle);
				} else {
					super.finishedRead();
				}
			}
		}
	}
	
	public void nextInstanceWillNotCache() {
		askForCaching = false;
	}
	
	@Override
	protected void setupHandler() {
		super.setupHandler();
		if (askForCaching) {
			if (isCompressing || super.seeUnpackedData()) {
				transfering = !con.getProxy().getAdaptiveEngine().transferResponse(con, response);
				if (log.isDebugEnabled())
					log.debug(this+" caching response data: "+!transfering);
			}
		}
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
	}
}
