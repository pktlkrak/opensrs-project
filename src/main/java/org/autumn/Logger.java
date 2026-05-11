/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.autumn;

/**
 * Simple logger interface
 * <p>The application implementing the interface should provide it itself
 * in order to keep all the logs in one place </p>
 * 
 * used by {@link Server}
 * @author piotr
 */
public interface Logger {
    /**
     * ID for messages used for debugging purposes
     */
    public static final int DEBUG = 0;
    
    /**
     * ID for messages used for informative purposes
     */
    public static final int INFO = 1;
    
    /**
     * ID for messages used for warnings
     */
    public static final int WARNING = 2;
    
    /**
     * ID for messages used for errors
     */
    public static final int ERROR = 3;
    
    /**
     * ID for messages used for application crashing errors
     */
    public static final int FATAL = 4;

    /**
     * Used for logging exceptions <br>
     * -- ex: example of using the log function, where ex is the exception got in catch -- <br>
     * <code>logger.logException(Logger.ERROR, "Autumn", ex, "The underlying endpoint has encountered a fatal error (" + path + ")");</code>
     * @param severity  one of the defined integers (ex. Logger.ERROR, Logger.FATAL)
     * @param tag       which module of the app logged the exception (ex. Autumn)
     * @param ex        the actual contents of the exception message
     * @param info      additional info provided by the app (ex. where the exception was thrown)
     */
    public void logException(int severity, String tag, Exception ex, String... info);
    
    /**
     * Used for logging non-exceptions <br>
     * -- ex: logging info about the server starting -- <br>
     * <code>logger.log(Logger.INFO, "Autumn", "Starting Autumn server at port " + port);</code>
     * @param severity one of the defined integers (ex. Logger.DEBUG, Logger.INFO)
     * @param tag      which module of the app logged the exception (ex. Autumn)
     * @param info     info provided by the app
     */
    public void log(int severity, String tag, String... info);
}
