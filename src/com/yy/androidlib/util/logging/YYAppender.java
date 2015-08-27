package com.yy.androidlib.util.logging;

import android.os.Environment;
import android.os.Process;
import android.util.Log;
import com.yy.androidlib.util.logging.Logger.Appender;
import com.yy.androidlib.util.logging.Logger.Level;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class YYAppender implements Appender {

    private static final String TAG = "YYAppender";

    @Override
    public void write(Level level, Object tag, String message) {
        switch (level) {
            case VERBOSE:
                verbose(tag, message);
                break;
            case DEBUG:
                debug(tag, message);
                break;
            case INFO:
                info(tag, message);
                break;
            case WARN:
                warn(tag, message);
                break;
            case ERROR:
                error(tag, message);
                break;
            default:
                Log.w(TAG, String.format("Unknown level to write log, level: %s, message: %s", level, message));
                break;
        }
    }

    /**
     * Log options.
     */
    public static class LogOptions {
        public static final int LEVEL_VERBOSE = 1;
        public static final int LEVEL_DEBUG = 2;
        public static final int LEVEL_INFO = 3;
        public static final int LEVEL_WARN = 4;
        public static final int LEVEL_ERROR = 5;

        /**
         * Uniform tag to be used as log tag; null-ok, if this is null, will use
         * the tag argument in log methods.
         */
        public String uniformTag;

        /**
         * When it is null, all stack traces will be output. Usually this can be
         * set the application package name.
         */
        public String stackTraceFilterKeyword;

        /**
         * The level at which the log method really works(output to DDMS and
         * file).
         * <p/>
         * NOTE this setting excludes the file writing of VERBOSE
         * except when set {@link #honorVerbose} to true explicitly.
         * If logLevel is LEVEL_VERBOSE:
         * a) when honorVerbose is true, will output all logs to DDMS and file.
         * b) when honorVerbose is false(default), will output all levels no less
         * than LEVEL_DEBUG to DDMS and file, but for verbose, will only output
         * to DDMS.
         * <p/>
         * <p/>
         * MUST be one of the LEVEL_* constants.
         */
        public int logLevel = LEVEL_INFO;

        public boolean honorVerbose = false;

        /**
         * Maximum backup log files' size in MB. Can be 0, which means no back
         * up logs(old logs to be discarded).
         */
        public int backUpLogLimitInMB = LogToES.DEFAULT_BAK_FILE_NUM_LIMIT * LogToES.MAX_FILE_SIZE;

        /**
         * Default file buffer size. Must be positive.
         */
        public int buffSizeInBytes = LogToES.DEFAULT_BUFF_SIZE;

        /**
         * Log file name, should not including the directory part. Must be a
         * valid file name(for Android file system).
         */
        public String logFileName = "logs.txt";
    }

    private volatile LogOptions sOptions = new LogOptions();

    private final ExecutorService THREAD = Executors.newSingleThreadExecutor();

    private int callerStackTraceIndex = 7;

    /**
     * @param directory Where to put the logs folder.
     * @param options   null-ok. Options for log methods.
     */
    public YYAppender(String directory, LogOptions options) {
        setOptions(options);
        LogToES.setLogPath(directory);
    }
    /**
     * @param directory Where to put the logs folder. Should be a writable directory.
     * @return True for succeeded, false otherwise.
     */
    public YYAppender(String directory) {
        LogToES.setLogPath(directory);
    }

    public static class LogOutputPaths {
        /**
         * The log directory, under which log files are put.
         */
        public String dir;

        /**
         * Current log file absolute file path. NOTE it may be empty.
         */
        public String currentLogFile;

        /**
         * Latest back up file path. null if there is none such file.
         */
        public String latestBackupFile;


    }

    /**
     * Get log output paths.
     *
     * @return null if not ready.
     */
    public LogOutputPaths getLogOutputPaths() {
        LogOutputPaths ret = new LogOutputPaths();
        if (!getLogOutputPaths(ret)) {

            Log.e("YYAppender", "failed to get log output paths.");
        }
        return ret;
    }

    /**
     * Get log output paths.
     *
     * @param out Output destination.
     * @return True for success, false otherwise.
     */
    public boolean getLogOutputPaths(LogOutputPaths out) {
        return LogToES.getLogOutputPaths(out, sOptions.logFileName);
    }

    /**
     * Make sure initialize is called before calling this.
     */
    public void setUniformTag(String tag) {
        if (tag != null && tag.length() != 0) {
            sOptions.uniformTag = tag;
        }
    }

    public String getLogPath() {
        return LogToES.getLogPath();
    }

    public LogOptions getOptions() {
        return sOptions;
    }

    private boolean setOptions(LogOptions options) {
        final LogOptions tmpOp = (options == null ? new LogOptions() : options);
        sOptions = tmpOp;
        LogToES.setBackupLogLimitInMB(tmpOp.backUpLogLimitInMB);
        LogToES.setBuffSize(tmpOp.buffSizeInBytes);
        return tmpOp.buffSizeInBytes > 0 && !isNullOrEmpty(tmpOp.logFileName);
    }

    /**
     * Output verbose log. Exception will be caught if input arguments have
     * format error.
     * <p/>
     *
     * @param obj
     * @param format The format string such as "This is the %d sample : %s".
     * @param args   The args for format.
     *               <p/>
     *               Reference : boolean : %b. byte, short, int, long, Integer, Long
     *               : %d. NOTE %x for hex. String : %s. Object : %s, for this
     *               occasion, toString of the object will be called, and the
     *               object can be null - no exception for this occasion.
     */
    public void verbose(Object obj, String format, Object... args) {
        final boolean shouldOutputVerboseToDDMS = shouldOutputVerboseToDDMS();
        final boolean shouldOutputVerboseToFile = shouldOutputVerboseToFile();
        if (shouldOutputVerboseToDDMS || shouldOutputVerboseToFile) {
            try {
                int line = getCallerLineNumber();
                String filename = getCallerFilename();
                outputVerbose(obj, line, filename, format, shouldOutputVerboseToDDMS, shouldOutputVerboseToFile, args);
            } catch (java.util.IllegalFormatException e) {
                Log.e(TAG, "write log error !", e);
            }
        }
    }

    /**
     * Output debug log. This version aims to improve performance by removing
     * the string concatenated costs on release version. Exception will be
     * caught if input arguments have format error.
     * <p/>
     *
     * @param obj
     * @param format The format string such as "This is the %d sample : %s".
     * @param args   The args for format.
     *               <p/>
     *               Reference : boolean : %b. byte, short, int, long, Integer, Long
     *               : %d. NOTE %x for hex. String : %s. Object : %s, for this
     *               occasion, toString of the object will be called, and the
     *               object can be null - no exception for this occasion.
     */
    public void debug(Object obj, String format, Object... args) {
        if (shouldWriteDebug()) {
            int line = getCallerLineNumber();
            String filename = getCallerFilename();
            outputDebug(obj, format, line, filename, args);
        }
    }

    /**
     * Output information log. Exception will be caught if input arguments have
     * format error.
     * <p/>
     *
     * @param obj
     * @param format The format string such as "This is the %d sample : %s".
     * @param args   The args for format.
     *               <p/>
     *               Reference : boolean : %b. byte, short, int, long, Integer, Long
     *               : %d. NOTE %x for hex. String : %s. Object : %s, for this
     *               occasion, toString of the object will be called, and the
     *               object can be null - no exception for this occasion.
     */
    public void info(Object obj, String format, Object... args) {
        if (shouldWriteInfo()) {
            try {
                int line = getCallerLineNumber();
                String filename = getCallerFilename();
                outputInfo(obj, format, line, filename, args);
            } catch (Exception e) {
                Log.e(TAG, "write log error !", e);
            }
        }
    }

    /**
     * Output warning log. Exception will be caught if input arguments have
     * format error.
     * <p/>
     *
     * @param obj
     * @param format The format string such as "This is the %d sample : %s".
     * @param args   The args for format.
     *               <p/>
     *               Reference : boolean : %b. byte, short, int, long, Integer, Long
     *               : %d. NOTE %x for hex. String : %s. Object : %s, for this
     *               occasion, toString of the object will be called, and the
     *               object can be null - no exception for this occasion.
     */
    public void warn(Object obj, String format, Object... args) {
        if (shouldWriteWarn()) {
            try {
                int line = getCallerLineNumber();
                String filename = getCallerFilename();
                outputWarning(obj, format, line, filename, args);
            } catch (Exception e) {
                Log.e(TAG, "write log error !", e);
            }
        }
    }

    /**
     * Output error log. Exception will be caught if input arguments have format
     * error.
     * <p/>
     *
     * @param obj
     * @param format The format string such as "This is the %d sample : %s".
     * @param args   The args for format.
     *               <p/>
     *               Reference : boolean : %b. byte, short, int, long, Integer, Long
     *               : %d. NOTE %x for hex. String : %s. Object : %s, for this
     *               occasion, toString of the object will be called, and the
     *               object can be null - no exception for this occasion.
     */
    public void error(Object obj, String format, Object... args) {
        if (shouldWriteError()) {
            try {
                int line = getCallerLineNumber();
                String filename = getCallerFilename();
                outputError(obj, format, line, filename, args);
            } catch (Exception e) {
                Log.e(TAG, "write log error !", e);
            }
        }
    }

    /**
     * Output an error log with contents of a Throwable.
     * <p/>
     *
     * @param t An Throwable instance.
     */
    public void error(Object obj, Throwable t) {
        if (shouldWriteError()) {
            int line = getCallerLineNumber();
            String filename = getCallerFilename();
            String methodname = getCallerMethodName();
            outputError(obj, t, line, filename, methodname);
        }
    }

    /**
     * Close the logging task. Flush will be called here. Failed to call this
     * may cause some logs lost.
     */
    public void close() {
        Runnable command = new Runnable() {
            @Override
            public void run() {
                if (externalStorageExist()) {
                    LogToES.close();
                }
            }
        };

        executeCommand(command);
    }

    private void executeCommand(final Runnable command) {
        THREAD.execute(command);
    }

    private String objClassName(Object obj) {
        if (obj instanceof String) {
            return (String) obj;
        } else {
            return obj.getClass().getSimpleName();
        }
    }

    private void writeToLog(final String logText) {
        final long timeMillis = System.currentTimeMillis();
        final Runnable command = new Runnable() {
            @Override
            public void run() {
                if (externalStorageExist()) {
                    try {
                        LogToES.writeLogToFile(LogToES.getLogPath(), sOptions.logFileName, logText, false, timeMillis);
                    } catch (Exception e) {
                        Log.e("YLogs", "writeToLog fail, " + e);
                    }
                }
            }
        };
        executeCommand(command);
    }

    private void logToFile(String logText, Throwable t) {
        StringWriter sw = new StringWriter();
        sw.write(logText);
        sw.write("\n");
        t.printStackTrace(new PrintWriter(sw));
        writeToLog(sw.toString());
    }

    private String msgForException(Object obj, String methodname, String filename, int line) {
        StringBuilder sb = new StringBuilder();
        if (obj instanceof String) {
            sb.append((String) obj);
        } else {
            sb.append(obj.getClass().getSimpleName());
        }
        sb.append(" Exception occurs at ");
        sb.append("(T:");
        sb.append(Thread.currentThread().getName());
        sb.append(")");
        sb.append(" at (");
        sb.append(filename);
        sb.append(":");
        sb.append(line);
        sb.append(")");
        return sb.toString();
    }

    private String msgForTextLog(Object obj, String filename, int line, String msg) {
        StringBuilder sb = new StringBuilder();
        sb.append(msg);
        sb.append(")");
        sb.append("(T:");
        sb.append(Thread.currentThread().getName());
        sb.append(")");
        sb.append(" at (");
        sb.append(filename);
        sb.append(":");
        sb.append(line);
        sb.append(")");
        return sb.toString();
    }

    public void setCallerStackTraceIndex(int index) {
        callerStackTraceIndex = index;
    }

    private int getCallerLineNumber() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        int index = callerStackTraceIndex;
        if (index >= elements.length) {
            index = elements.length - 1;
        }
        return elements[index].getLineNumber();
    }

    private String getCallerFilename() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        int index = callerStackTraceIndex;
        if (index >= elements.length) {
            index = elements.length - 1;
        }
        return elements[index].getFileName();
    }

    private String getCallerMethodName() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        int index = callerStackTraceIndex;
        if (index >= elements.length) {
            index = elements.length - 1;
        }
        return elements[index].getMethodName();
    }

    private String getThreadStacksKeyword() {
        return sOptions.stackTraceFilterKeyword;
    }

    public void printThreadStacks() {
        printThreadStacks(tagOfStack(), getThreadStacksKeyword(), false, false);
    }

    public void printThreadStacks(String tag) {
        printThreadStacks(tag, getThreadStacksKeyword(), isNullOrEmpty(getThreadStacksKeyword()), false);
    }

    public void printThreadStacks(Throwable e, String tag) {
        printStackTraces(e.getStackTrace(), tag);
    }

    public void printThreadStacks(String tag, String keyword) {
        printThreadStacks(tag, keyword, false, false);
    }

    // tag is for output identifier.
    // keyword is for filtering irrelevant logs.
    public void printThreadStacks(String tag, String keyword, boolean fullLog, boolean release) {
        printStackTraces(Thread.currentThread().getStackTrace(), tag, keyword, fullLog, release);
    }

    public void printStackTraces(StackTraceElement[] traces, String tag) {
        printStackTraces(traces, tag, getThreadStacksKeyword(), isNullOrEmpty(sOptions.stackTraceFilterKeyword), false);
    }

    private void printStackTraces(StackTraceElement[] traces, String tag, String keyword, boolean fullLog, boolean release) {
        printLog(tag, "------------------------------------", release);
        for (StackTraceElement e : traces) {
            String info = e.toString();
            if (fullLog || (!isNullOrEmpty(keyword) && info.contains(keyword))) {
                printLog(tag, info, release);
            }
        }
        printLog(tag, "------------------------------------", release);
    }

    private void printLog(String tag, String log, boolean release) {
        if (release) {
            info(tag, log);
        } else {
            debug(tag, log);
        }
    }

    private String tag(Object tag) {
        final LogOptions options = sOptions;
        return (options.uniformTag == null ? (tag instanceof String ? (String) tag : tag.getClass().getSimpleName()) : options.uniformTag);
    }

    private String tagOfStack() {
        return (sOptions.uniformTag == null ? "CallStack" : sOptions.uniformTag);
    }

    private boolean shouldOutputVerboseToDDMS() {
        return sOptions.logLevel <= LogOptions.LEVEL_VERBOSE;
    }

    private boolean shouldOutputVerboseToFile() {
        return sOptions.logLevel <= LogOptions.LEVEL_VERBOSE && sOptions.honorVerbose;
    }

    private boolean shouldWriteDebug() {
        return sOptions.logLevel <= LogOptions.LEVEL_DEBUG;
    }

    private boolean shouldWriteInfo() {
        return sOptions.logLevel <= LogOptions.LEVEL_INFO;
    }

    private boolean shouldWriteWarn() {
        return sOptions.logLevel <= LogOptions.LEVEL_WARN;
    }

    private boolean shouldWriteError() {
        return sOptions.logLevel <= LogOptions.LEVEL_ERROR;
    }

    private boolean externalStorageExist() {
        return Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED);
    }

    private boolean isNullOrEmpty(String s) {
        return s == null || s.length() == 0;
    }

    private void outputVerbose(final Object obj, final int line, final String filename, final String format, boolean outToDDMS, boolean outToFile, final Object... args) {
        try {
            String msg = (args == null || args.length == 0) ? format : String.format(format, args);
            String logText = msgForTextLog(obj, filename, line, msg);
            if (outToDDMS) {
                Log.v(tag(obj), logText);
            }
            if (outToFile) {
                writeToLog(logText);
            }
        } catch (Exception e) {
            Log.e(TAG, "write log error !", e);
        }
    }

    private void outputDebug(final Object obj, final String format, final int line, final String filename, final Object... args) {
        try {
            String msg = (args == null || args.length == 0) ? format : String.format(format, args);
            String logText = msgForTextLog(obj, filename, line, msg);
            Log.d(tag(obj), logText);
            writeToLog(logText);
        } catch (Exception e) {
            Log.e(TAG, "write log error !", e);
        }
    }

    private void outputInfo(final Object obj, final String format, final int line, final String filename, final Object... args) {
        try {
            String msg = (args == null || args.length == 0) ? format : String.format(format, args);
            String logText = msgForTextLog(obj, filename, line, msg);
            Log.i(tag(obj), logText);
            writeToLog(logText);
        } catch (Exception e) {
            Log.e(TAG, "write log error !", e);
        }
    }

    private void outputWarning(final Object obj, final String format, final int line, final String filename, final Object... args) {
        try {
            String msg = (args == null || args.length == 0) ? format : String.format(format, args);
            String logText = msgForTextLog(obj, filename, line, msg);
            Log.w(tag(obj), logText);
            writeToLog(logText);
        } catch (Exception e) {
            Log.e(TAG, "write log error !", e);
        }
    }

    private void outputError(final Object obj, final String format, final int line, final String filename, final Object... args) {
        try {
            String msg = (args == null || args.length == 0) ? format : String.format(format, args);
            String logText = msgForTextLog(obj, filename, line, msg);
            // If the last arg is a throwable, print the stack.
            if (args != null && args.length > 0 && args[args.length - 1] instanceof Throwable) {
                Throwable t = (Throwable) args[args.length - 1];
                Log.e(tag(obj), logText, t);
                logToFile(logText, t);
            } else {
                Log.e(tag(obj), logText);
                writeToLog(logText);
            }
        } catch (Exception e) {
            Log.e(TAG, "write log error !", e);
        }
    }

    private void outputError(final Object obj, final Throwable t, final int line, final String filename, final String methodname) {
        try {
            String logText = msgForException(obj, methodname, filename, line);
//            Log.e(tag(obj), logText, t);
            logToFile(logText, t);
        } catch (Exception e) {
            Log.e(TAG, "write log error !", e);
        }

    }
}
