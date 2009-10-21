package rabbit.proxy;

import java.util.HashMap;
import java.util.Map;
import rabbit.handler.HandlerFactory;
import rabbit.util.Config;
import rabbit.util.Logger;
import rabbit.util.SProperties;

/** A class to handle mime type handler factories.
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
class HandlerFactoryHandler {
    private Map<String, HandlerFactory> handlers;
    private Map<String, HandlerFactory> cacheHandlers;
    private Map<String, HandlerFactory> handlersForNames;
    
    public HandlerFactoryHandler (SProperties handlersProps, 
				  SProperties cacheHandlersProps, 
				  Config config,
				  Logger log) {
    handlersForNames = new HashMap<String, HandlerFactory>();
	handlers = loadHandlers (handlersProps, config, log);
	cacheHandlers = loadHandlers (cacheHandlersProps, config, log);
    }

    /** load a set of handlers.
     * @param section the section in the config file.
     * @param log the Logger to write errors/warnings to.
     * @return a Map with mimetypes as keys and Handlers as values.
     */
    protected Map<String, HandlerFactory> 
	loadHandlers (SProperties handlersProps, Config config, Logger log) {
	Map<String, HandlerFactory> hhandlers = 
	    new HashMap<String, HandlerFactory> ();
	if (handlersProps == null)
	    return hhandlers;
	for (String mime : handlersProps.keySet ()) {
	    HandlerFactory hf;
	    String id = handlersProps.getProperty (mime).trim ();
	    // simple regexp like expansion,
	    // first '?' char indicates optional prev char
	    int i = mime.indexOf ('?');
	    if (i <= 0) {
		// no '?' found, or it is the first char
		hf = setupHandler (id, config, log, mime);
		hhandlers.put (mime, hf);
	    } else {
		// remove '?'
		mime = mime.substring (0, i) + mime.substring (i + 1);
		hf = setupHandler (id, config, log, mime);
		hhandlers.put (mime, hf);
		// remove the optional char
		String mime2 = 
		    mime.substring (0, i - 1) + mime.substring (i);
		hf = setupHandler (id, config, log, mime2);
		hhandlers.put (mime2, hf);
	    }
	}
	return hhandlers;
    }

    private HandlerFactory setupHandler (String id, Config config, 
					 Logger log, String mime) {
	String className = id;
	HandlerFactory hf = handlersForNames.get(id);
    if (hf != null) {
    	return hf;
    }
	try {
	    int i = id.indexOf ('*');
	    if (i >= 0)
		className = id.substring (0, i);
	    Class<? extends HandlerFactory> cls = 
		Class.forName (className).asSubclass (HandlerFactory.class);
	    hf = cls.newInstance ();
	    hf.setup (log, config.getProperties (id));
	} catch (ClassNotFoundException ex) {
	    log.logError ("Could not load class: '" + className
			  + "' for handlerfactory '" + mime + "'");
	} catch (InstantiationException ie) {
	    log.logError ("Could not instanciate factory class: '" + 
			  className + "' for handler '" + 
			  mime + "' :" + ie);
	} catch (IllegalAccessException iae) {
	    log.logError ("Could not instanciate factory class: '" + 
			  className + "' for handler '" + 
			  mime + "' :" + iae);
	}
	handlersForNames.put(id, hf);
	return hf;
    }

    HandlerFactory getHandlerFactory (String mime) {
	HandlerFactory retVal = handlers.get (mime);
	if (retVal == null) {
		int lastMatchLength = 0;
		for (String mimeInMap : handlers.keySet()) {
			if (mime.startsWith(mimeInMap) && lastMatchLength < mimeInMap.length()) {
				retVal = handlers.get(mimeInMap);
				lastMatchLength = mimeInMap.length();
			}
		}
	}
    return retVal;
    }

    HandlerFactory getCacheHandlerFactory (String mime) {
	return cacheHandlers.get (mime);
    }
    
    HandlerFactory getNamedHandleFactory(String handlerName) {
    	return handlersForNames.get(handlerName);
    }
}
