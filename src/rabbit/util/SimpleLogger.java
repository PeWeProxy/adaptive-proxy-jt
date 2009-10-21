package rabbit.util;

/** A logger that logs to stdout/stderr.
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public class SimpleLogger implements Logger {
    /** The current error log level. */
    private Level logLevel = Level.MSG;

    public boolean showsLevel (Level level) {
	return logLevel.compareTo (level) <= 0;
    }

    public void setLogLevel (Level level) {
	logLevel = level;
    }

    public void logDebug (String error) {
	logError (Level.DEBUG, error);
    }

    public void logAll (String error) {
	logError (Level.ALL, error);	
    }

    public void logInfo (String error) {
	logError (Level.INFO, error);
    }

    public void logWarn (String error) {
	logError (Level.WARN, error);	
    }

    public void logMsg (String error) {
	logError (Level.MSG, error);	
    }

    public void logError (String error) {
	logError (Level.ERROR, error);	
    }

    public void logFatal (String error) {
	logError (Level.FATAL, error);	
    }

    public void logError (Level level, String error) {
	if (!showsLevel (level))
	    return;	
	System.err.println ("[" + level + "][" + error + "]");
    }
    
    public void rotateLogs () {
	// nothing
    }
}
