package rabbit.util;

/** The logging levels. 
 *
 * @author <a href="mailto:robo@khelekore.org">Robert Olofsson</a>
 */
public enum Level implements Comparable<Level> {
    /** Show all messages in the log. */
    DEBUG,
    /** Show all normal messages and higher. */
    ALL,
    /** Show information messages and higher. */
    INFO,
    /** Show warnings and higer. */
    WARN,
    /** Show important messages and above. */
    MSG,
    /** Show error messages and higer. */
    ERROR,
    /** Show only fatal messages. */
    FATAL;
}
