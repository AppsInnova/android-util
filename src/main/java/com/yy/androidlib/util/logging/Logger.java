package com.yy.androidlib.util.logging;

import android.text.TextUtils;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: lxl
 * Date: 7/4/14
 * Time: 9:50 AM
 */
public class Logger {

    public enum Level {
        VERBOSE, DEBUG, INFO, WARN, ERROR,
    }

    public interface Appender {
        void write(Level level, Object tag, String message);
    }

    private static final List<Appender> APPENDERS = new ArrayList<Appender>();

    public static void init(Appender... appenders) {
        Collections.addAll(APPENDERS, appenders);
    }

    public static void verbose(Object tag, String format, Object... args) {
        writeLog(Level.VERBOSE, tag, format, args);
    }

    public static void debug(Object tag, String format, Object... args) {
        writeLog(Level.DEBUG, tag, format, args);
    }

    public static void info(Object tag, String format, Object... args) {
        writeLog(Level.INFO, tag, format, args);
    }

    public static void warn(Object tag, String format, Object... args) {
        writeLog(Level.WARN, tag, format, args);
    }

    public static void error(Object tag, String format, Object... args) {
        writeLog(Level.ERROR, tag, format, args);
    }

    public static void error(Object tag, Throwable t) {
        writeLog(Level.ERROR, tag, stackTraceOf(t));
    }

    public static String stackTraceOf(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    public static String stackTrace() {
        StackTraceElement[] traces = Thread.currentThread().getStackTrace();
        return TextUtils.join("\n", traces);
    }

    private static void writeLog(Level level, Object tag, String format, Object... args) {
        try {
            String message = (args == null || args.length == 0) ? format : String.format(format, args);
            if (args != null && args.length > 0 && args[args.length - 1] instanceof Throwable) {
                Throwable throwable = (Throwable) args[args.length - 1];
                message = getExceptionLogText(message, throwable);
            }
            for (Appender appender : APPENDERS) {
                appender.write(level, tag, message);
            }
        } catch (Exception e) {
            Log.e("Logger", String.format("write log failed: %s", e.toString()));
            e.printStackTrace();
        }
    }

    private static String getExceptionLogText(String message, Throwable throwable) {
        StringWriter sw = new StringWriter();
        sw.write(message);
        sw.write("\n");
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private static String tag(Object tag) {
        return (tag instanceof String ? (String) tag : tag.getClass().getSimpleName());
    }

}
