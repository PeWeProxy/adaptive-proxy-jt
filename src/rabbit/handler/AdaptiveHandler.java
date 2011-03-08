package rabbit.handler;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import rabbit.filter.HtmlFilterFactory;
import rabbit.http.HttpHeader;
import rabbit.httpio.ResourceSource;
import rabbit.httpio.request.ContentChunksModifier;
import rabbit.httpio.request.ContentChunksModifier.AsyncChunkDataModifiedListener;
import rabbit.httpio.request.ContentChunksModifier.AsyncChunkModifierListener;
import rabbit.io.BufferHandle;
import rabbit.io.SimpleBufferHandle;
import rabbit.proxy.Connection;
import rabbit.proxy.TrafficLoggerHandler;
import sk.fiit.peweproxy.utils.HeaderUtils;
import sk.fiit.peweproxy.utils.InMemBytesStore;

public class AdaptiveHandler extends FilterHandler {
	private static final org.apache.log4j.Logger log
		= org.apache.log4j.Logger.getLogger(AdaptiveHandler.class);
	
	/**
	 * whether are we sending data that are read 
	 */
	private boolean transfering = true;
	/**
	 * whether are we sending passed response
	 */
	private boolean sendingPhase = false;
	private ByteBuffer buffer = null;
	private boolean askForCaching = true;
	private boolean doHTMLparsing = false;
	private ContentChunksModifier chunksModifier = null;
	private boolean finishAfterSend = false;
	private byte[] chunkProcessedData = null;
	
	public AdaptiveHandler() {}
	
	public AdaptiveHandler(Connection con, TrafficLoggerHandler tlh,
			HttpHeader header, HttpHeader webHeader,
			ResourceSource content, boolean mayCache, boolean mayFilter,
			long size, boolean compress, boolean repack,
			  List<HtmlFilterFactory> filterClasses) {
		super(con, tlh, header, webHeader, content, mayCache,
				mayFilter, size, compress, repack, filterClasses);
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
				if (!sendingPhase) {
					//  modify if needed and cache for late processing
					chunksModifier.modifyArray(arr, off, len, new AsyncChunkDataModifiedListener() {
						@Override
						public void dataModified(byte[] newData) {
							if (newData == null)
								sendArray(null, 0, 0);
							else {
								sendArray(newData, 0, newData.length);
							}
						}
					});
				} else {
					sendArray(arr, off, len);
				}
			}
		} else {
			// modify if needed and cache for full access real-time processing 
			chunksModifier.modifyArray(arr, off, len, new AsyncChunkDataModifiedListener() {
				@Override
				public void dataModified(byte[] newData) {
					blockSent();
				}
			});
		}
	}
	
	private void sendArray(byte[] arr, int off, int len) {
		if (len == 0)
			blockSent();
		else {
			List<ByteBuffer> buffers = new LinkedList<ByteBuffer>();
			buffers.add(ByteBuffer.wrap(arr, off, len));
			sendBlocks = buffers.iterator();
			sendBlockBuffers();
		}
	}

	@Override
	protected void finishData() {
		if (transfering || sendingPhase) {
			if (!sendingPhase && con.getChunking()) {
				// data cached for late processing, if we can, send remaining chunks
				chunksModifier.finishedRead(new AsyncChunkDataModifiedListener() {
					@Override
					public void dataModified(byte[] newData) {
						if (newData != null && newData.length > 0) {
							finishAfterSend = true;
							chunkProcessedData = newData;
							sendArray(newData, 0, newData.length);
						} else
							AdaptiveHandler.super.finishData();
					}
				});
			} else {
				// sending provided response, or we can't modify response (non-chunked transfer)
				super.finishData();
			}
		} else {
			// data cached for full access real-time processing, it will initiate sending
			// so we must ignore con.getChunking() 
			chunksModifier.finishedRead(null);
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
		sendingPhase = true;
		writeBytes = true;
		totalRead = 0;
		setHTMLparsing();
		sendHeader();
	}
	
	@Override
	protected void send(BufferHandle bufHandle) {
		boolean modify = true;
		if (bufHandle.getBuffer().array() == chunkProcessedData) {
			modify = false;
			chunkProcessedData = null;
		}
		if (modify && doHTMLparsing && transfering && !sendingPhase) {
			// FilterHandler's methods are messing with HTML blocks
			// 		&& we are not caching for full access processing
			//		&& we are not sending something already cached
			chunksModifier.modifyBuffer(bufHandle, new AsyncChunkModifierListener() {
				@Override
				public void chunkModified(BufferHandle bufHandle) {
					if (bufHandle.isEmpty())
						blockSent();
					else
						AdaptiveHandler.super.send(bufHandle);
				}
			});
			return;
		}
		super.send(bufHandle);
	}
	
	@Override
	protected void requestMoreData() {
		if (finishAfterSend)
			super.finishData();
		else if (!sendingPhase)
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
				if (buffer.hasRemaining()) {
					super.bufferRead(new SimpleBufferHandle(InMemBytesStore.chunkBufferForSend(buffer)));
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
	
	public void setChunksListener(ContentChunksModifier chunksModifier) {
		this.chunksModifier = chunksModifier;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
	}
}
