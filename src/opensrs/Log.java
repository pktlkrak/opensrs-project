/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs;

/**
 *
 * @author piotr
 */
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author piotr
 */
public class Log {

    private static final String TAG = "Log";
    
    private static File logFile = null;

    public static void setLogFile(File newLogFile) {
        Log.logFile = newLogFile;
    }

    public static final int DEBUG = 0;
    public static final int INFO = 1;
    public static final int WARNING = 2;
    public static final int ERROR = 3;
    public static final int FATAL = 4;

    private static boolean threadRunning = false;
    private static boolean prepareForExit = false;
    private static Thread loggingThread;
    private static final Object LOCKING_OBJECT = new Object();
    private static String filterString = null;

    public static void startLoggingThread() {
        if (threadRunning) {
            return;
        }
        threadRunning = true;
        loggingThread = new Thread(() -> {
            runLoggingThread();
        }, "Logging thread");
        loggingThread.start();
    }

    public static void prepareForExit() throws InterruptedException {
        prepareForExit = true;
        log(Log.INFO, TAG, "Preparing for exit.");
        loggingThread.join();
    }

    public static void setFilters(String filters) {
        log(Log.INFO, TAG, "TAG Filtering set from " + filterString + " to " + filters);
        filterString = filters;
    }

    private static Queue<LogRepresentation> queue = new ConcurrentLinkedQueue<>();

    private static void writeLog(LogRepresentation logObject) {
        String colorTag = "";
        switch (logObject.severity) {
            case DEBUG:
                colorTag = TextColors.ANSI_CYAN;
                break;
            case INFO:
                colorTag = TextColors.ANSI_GREEN;
                break;
            case WARNING:
                colorTag = TextColors.ANSI_YELLOW;
                break;
            case ERROR:
                colorTag = TextColors.ANSI_RED;
                break;
            case FATAL:
                colorTag = TextColors.ANSI_BLUE;
                break;
        }
        if (logObject.content == null) {
            return;
        }

        String entry = "";
        String severityString = decodeSeverity(logObject.severity);
        
        if(logObject.tag.equals("INIT") || logObject.tag.equals("PREINIT")){
            logObject.tag = TextColors.ANSI_PURPLE + logObject.tag + colorTag;
        }
        for (String content : logObject.content) {
            for (String line : content.split("\n")) {
                entry
                        += colorTag
                        + logObject.date + ": "
                        + severityString
                        + ": [" + logObject.tag + "]: "
                        + line + "\n";
            }
        }

        if (logObject.storeInFile && logFile != null) {
            String contentToWrite = entry.replace(colorTag, "").replace(TextColors.ANSI_RESET, "");
            try {
                Files.write(logFile.toPath(), contentToWrite.getBytes(), StandardOpenOption.APPEND);
            } catch (IOException ex) {
                try {
                    Files.write(logFile.toPath(), contentToWrite.getBytes(), StandardOpenOption.CREATE_NEW);
                } catch (IOException ex1) {
                    log(ERROR, TAG, false, "Log internal error(can't create logfile) :", ex1.getMessage()); //How nice
                }
            }
        }
        
        System.out.println(entry.substring(0, entry.length() - 1).replace("\n", "\n" + colorTag) + TextColors.ANSI_RESET);
    }

    private static void runLoggingThread() {
        log(Log.INFO, TAG, "Starting the log thread...");
        synchronized (LOCKING_OBJECT) {
            while (!prepareForExit) {
                try {
                    LOCKING_OBJECT.wait();
                } catch (InterruptedException ex) {
                }
                
                while (queue.size() > 0) {
                    writeLog(queue.poll());
                }
            }
        }
    }

    private static String decodeSeverity(int s) {
        switch (s) {
            case DEBUG:
                return "[D]";
            case INFO:
                return "[I]";
            case WARNING:
                return "[W]";
            case ERROR:
                return "[E]";
            case FATAL:
                return "[F]";
        }
        return "[INCORRECT_SEVERITY_GIVEN]";
    }

    public static void log(int severity, String tag, boolean storeInFile, String... data) {
        startLoggingThread();
        queue.add(new LogRepresentation(tag, severity, data, storeInFile));
        synchronized (LOCKING_OBJECT) {
            LOCKING_OBJECT.notify();
        }
    }

    public static void log(int severity, String tag, String... data) {
        log(severity, tag, true, data);
    }

    public static void log(String tag, String... data) {
        log(INFO, tag, true, data);
    }

    public static void log(String tag, boolean storeInFile, String... data) {
        log(INFO, tag, storeInFile, data);
    }

    public static void handleException(int severity, String tag, boolean storeInFile, Exception x, String... additionalData) {
        String sum = "";
        for (String a : additionalData) {
            sum += a + "\n";
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        x.printStackTrace(pw);
        String stackTrace = sw.toString();
        log(severity, "EXCEPTION:" + tag, storeInFile, x.getMessage() + "\n" + (sum.isEmpty() ? "" : ("Additional data: \n" + sum)) + stackTrace);
    }

    public static String getExcpetion(Exception ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }

    public static void handleException(int severity, String tag, Exception data, String... adData) {
        handleException(severity, tag, true, data, adData);
    }

    public static void handleException(String tag, Exception data, String... adData) {
        handleException(ERROR, tag, true, data, adData);
    }

    public static void handleException(String tag, boolean storeInFile, Exception data, String... adData) {
        handleException(ERROR, tag, storeInFile, data, adData);
    }

    private static class LogRepresentation {

        private String date, tag;
        private int severity;
        private boolean storeInFile = true;
        private String content[];

        private void updateDate() {
            date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        }

        public LogRepresentation(String tag, int severity, String[] content, boolean storeInFile) {
            this.tag = tag;
            this.storeInFile = storeInFile;
            this.severity = severity;
            this.content = content;
            updateDate();
        }

        public boolean isStoreInFile() {
            return storeInFile;
        }

        public String getDate() {
            return date;
        }

        public String getTag() {
            return tag;
        }

        public int getSeverity() {
            return severity;
        }

        public String[] getContent() {
            return content;
        }

    }
}
