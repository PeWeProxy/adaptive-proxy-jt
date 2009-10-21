package sk.fiit.rabbit.adaptiveproxy;

import rabbit.util.Level;
import rabbit.util.Logger;

public class Log4jLogger implements Logger {
	private final org.apache.log4j.Logger logger;
	
	public Log4jLogger(org.apache.log4j.Logger logger) {
		this.logger = logger;
	}
	
	org.apache.log4j.Level getLvl4Lvl(Level level) {
    	org.apache.log4j.Level retVal = null;
		switch (level) {
    	case DEBUG:
    	case ALL:
			retVal = org.apache.log4j.Level.DEBUG;
			break;
    	case INFO:
			retVal = org.apache.log4j.Level.INFO;
			break;
    	case WARN:
			retVal = org.apache.log4j.Level.WARN;
			break;
    	case MSG:
    	case ERROR:
			retVal = org.apache.log4j.Level.ERROR;
			break;
    	case FATAL:
			retVal = org.apache.log4j.Level.FATAL;
			break;
		default:
			retVal = org.apache.log4j.Level.DEBUG;
			break;
		}
		return retVal;
    }
	

	@Override
	public void logAll(String error) {
		logError(Level.ALL, error);
	}

	@Override
	public void logDebug(String error) {
		logger.debug(error);
	}

	@Override
	public void logError(String error) {
		logError(Level.ERROR, error);
	}

	@Override
	public void logError(Level level, String error) {
		logger.log(getLvl4Lvl(level), error);
	}

	@Override
	public void logFatal(String error) {
		logError(Level.FATAL, error);
	}

	@Override
	public void logInfo(String error) {
		logError(Level.INFO, error);
	}

	@Override
	public void logMsg(String error) {
		logError(Level.MSG, error);
	}

	@Override
	public void logWarn(String error) {
		logError(Level.WARN, error);
	}

	@Override
	public void rotateLogs() {
		// no-op
	}

	@Override
	public boolean showsLevel(Level level) {
		return logger.isEnabledFor(getLvl4Lvl(level));
	}

}
