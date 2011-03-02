package rabbit.httpio.request;

import rabbit.httpio.BlockListener;


/**
 * Base class for content data source classes - entities that provides
 * bytes of HTTP message content. All provided data MUST be the data of one message
 * content, that is the implementation of this abstraction should NEVER
 * pass some bytes that do not belong to single HTTP message content. 
 * @author Redeemer
 *
 */
public abstract class ContentSource {
	protected BlockListener listener;
	
	
	public void readFirstBytes(BlockListener listener) {
		this.listener = listener;
		readNextBytes();
	}
	
	/**
	 * Tells this content source to gather some more content data. When
	 * it obtains some more data, {@link BlockListener#bufferRead()}
	 * of associated listener will be called.
	 */
	abstract void readNextBytes();
}
