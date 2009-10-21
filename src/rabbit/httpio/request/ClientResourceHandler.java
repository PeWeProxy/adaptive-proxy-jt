package rabbit.httpio.request;

import java.io.IOException;
import rabbit.http.HttpHeader;
import rabbit.httpio.BlockListener;
import rabbit.httpio.BlockSender;
import rabbit.httpio.BlockSentListener;
import rabbit.httpio.ChunkEnder;
import rabbit.io.BufferHandle;
import rabbit.io.WebConnection;
import rabbit.proxy.ClientResourceTransferredListener;
import rabbit.proxy.Connection;

public class ClientResourceHandler implements BlockListener {
	protected final Connection con;
    protected final ContentSource contentSource;
    private final boolean chunking;
    private boolean sentEndChunk = false;
    protected WebConnection wc;
    protected ClientResourceTransferredListener listener;
    final SendingListener sendingListener;
    

    public ClientResourceHandler(Connection con, ContentSource contentSource,
    		boolean chunking) {
    	this.con = con;
		this.contentSource = contentSource;
		this.chunking = chunking;
		sendingListener = new SendingListener();
    }
    
    /** Modify the request sent to the server, used to add 
     *  "Expect: 100 Continue" and similar. 
     * @param header the HttpHeader to be modified by this client request. 
     */
    public void modifyRequest (HttpHeader header) {
    	if (chunking)
    		header.setHeader ("Transfer-Encoding", "chunked");
    }
    
    /** Transfer the resouce data
     * @param wc the web connection to send the resource to
     * @param crtl the listener that want to know when the resource
     *             have been sent or when a failure have occurred.
     */
    public void transfer(WebConnection wc, ClientResourceTransferredListener crtl) {
		this.wc = wc;
		this.listener = crtl;
		contentSource.readFirstBytes(this);
    }
    
    @Override
    public void timeout() {
    	listener.timeout ();
    }

    @Override
    public void failed(Exception e) {
    	listener.failed (e);
    }
    
    @Override
    public void bufferRead(BufferHandle bufHandle) {
    	try {
    	    new BlockSender (wc.getChannel (), con.getSelector (), 
    			     con.getLogger (), con.getTrafficLoggerHandler().getNetwork (),
    			     bufHandle, chunking, sendingListener);
    	} catch (IOException e) {
    	    failed (e);
    	}
    }
    
    @Override
    public void finishedRead() {
    	if (!chunking)
			listener.clientResourceTransferred();
    	else
    		try {
				ChunkEnder ce = new ChunkEnder ();
				sentEndChunk = true;	
				ce.sendChunkEnding (wc.getChannel(), con.getSelector(),
						con.getLogger(), con.getTrafficLoggerHandler().getNetwork(),
						sendingListener);
			} catch (IOException e) {
				failed(e);
			}
    }
    
    class SendingListener implements BlockSentListener {
		@Override
		public void blockSent() {
			if (sentEndChunk)
	    		listener.clientResourceTransferred();
	    	else
	    		contentSource.readNextBytes();
		}

		@Override
		public void failed(Exception cause) {
			listener.sendingFailed(cause);
		}

		@Override
		public void timeout() {
			listener.sendingTimeout();
		}
    }
}
