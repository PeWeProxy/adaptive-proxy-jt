package rabbit.io;

import java.nio.channels.SelectionKey;

/** A class to hold information about when an operation 
 *  was attached to the selector. Needed for timeout options.
 */
public interface HandlerRegistration {
    /** Get the time set as registration time */
    long getRegistrationTime ();

    /** Check if this registration is expired. 
     */
    boolean isExpired (long now, long timeout);

    /** Get the handler for the key.
     */
    SocketHandler getHandler (SelectionKey sk);

    /** Registers a new handler to the given selection key.
     * @param currentOps the current interest ops
     * @param newOps the ops to handle
     * @param sk the current channel key.
     * @param sh the new handler to register.
     * @param when the new timeout timing
     */
    void register (int currentOps, int newOps, SelectionKey sk, 
		   SocketHandler sh, long when);
    
    /** unregister the given handler.
     * @param sk the current channel key.
     * @param sh the handler to unregister.
     * @param reason a text message saying why the unregistration happened.
     */
    void unregister (SelectionKey sk, SocketHandler sh, String reason);       

    /** This handler has timed out.
     */
    void timeout ();

    /** Get a text description 
     */
    String getDescription ();
}
    
