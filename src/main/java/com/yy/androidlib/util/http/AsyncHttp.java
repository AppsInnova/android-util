package com.yy.androidlib.util.http;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;
import com.yy.androidlib.util.logging.Logger;

import org.apache.http.Header;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

public class AsyncHttp {

    private static final String TAG = "AsyncHttp";
    private static AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
    private static Profiler profiling;

    public static void enableProfiling(Profiler profiling) {
        AsyncHttp.profiling = profiling;
    }

    public static void post(final String url, final List<FormEntry> entries, final ResultCallback callback, final Header... headers) {

        RequestParams params = new RequestParams();
        for (FormEntry entry : entries) {
            switch (entry.type) {
                case File:
                    try {
                        params.put(entry.name, new File(entry.value));
                    } catch (FileNotFoundException e) {
                        Logger.error(TAG, "file not found %s", entry.value, e);
                    }
                    break;
                default:
                    if (entry instanceof FileDataFormEntry) {
                        FileDataFormEntry fileDataFormEntry = (FileDataFormEntry) entry;
                        params.put(fileDataFormEntry.name, fileDataFormEntry.byteValue);
                    } else {
                        params.put(entry.name, entry.value);
                    }
                    break;
            }
        }


        final long startTime = System.currentTimeMillis();
        asyncHttpClient.post(null, url, headers, params, null, new TextHttpResponseHandler() {
            @Override
            public void onFailure(final int statusCode, Header[] headers, final String responseString, Throwable throwable) {
                Logger.error(TAG, "http request error, statusCode: %d, responseString: %s, url: %s, thread: %s",
                        statusCode, responseString, url, Thread.currentThread().getName(), throwable);
                callback.onFailure(url, statusCode, ResultCallback.EXCEPTION, throwable);
                report(startTime, url, Profiler.Status.ERROR.value);

            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                callback.onSuccess(url, statusCode, responseString);
                report(startTime, url, Profiler.Status.SUCCESS.value);

            }
        });
    }

    /**
     * use {@link #post(String, java.util.List, com.yy.androidlib.util.http.AsyncHttp.ResultCallback, org.apache.http.Header...)} instead
     *
     * @param url
     * @param entries
     * @param callback
     * @param headers
     */
    @Deprecated
    public static void post(final String url, final List<FormEntry> entries, final Callback callback, final Header... headers) {
        post(url, entries, transferToNewCallback(callback), headers);
    }


    public static void download(final String url, File file, final ResultCallback callback) {
        final long startTime = System.currentTimeMillis();
        asyncHttpClient.get(url, null, new FileAsyncHttpResponseHandler(file) {
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                callback.onFailure(url, statusCode, ResultCallback.EXCEPTION, throwable);
                report(startTime, url, Profiler.Status.ERROR.value);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, File response) {
                callback.onSuccess(url, statusCode, "");
                report(startTime, url, Profiler.Status.SUCCESS.value);
            }
        });
    }

    /**
     * use {@link #download(String, java.io.File, com.yy.androidlib.util.http.AsyncHttp.ResultCallback)} instead
     *
     * @param url
     * @param file
     * @param callback
     */
    @Deprecated
    public static void download(final String url, File file, final Callback callback) {
        download(url, file, transferToNewCallback(callback));
    }

    public interface ResultCallback {
        int NETWORK_NOT_AVAILABLE = -1;
        int URL_NULL = -2;
        int EXCEPTION = -3;

        void onSuccess(String url, int statusCode, String result);

        void onFailure(String url, int statusCode, int errorType, Throwable throwable);
    }

    /**
     * use {@link com.yy.androidlib.util.http.AsyncHttp.ResultCallback} instead
     */
    @Deprecated
    public interface Callback {
        int NETWORK_NOT_AVAILABLE = -1;
        int URL_NULL = -2;
        int EXCEPTION = -3;

        void onResult(String url, boolean success, int mStatusCode, String result);
    }

    private static ResultCallback transferToNewCallback(final Callback callback) {
        return new ResultCallback() {
            @Override
            public void onSuccess(String url, int statusCode, String result) {
                callback.onResult(url, true, statusCode, result);
            }

            @Override
            public void onFailure(String url, int statusCode, int errorType, Throwable throwable) {
                callback.onResult(url, false, errorType, "");
            }
        };
    }

    public static void get(final String url, final ResultCallback callback, final Header... headers) {

        RequestParams params = new RequestParams();
        final long startTime = System.currentTimeMillis();
        asyncHttpClient.get(null, url, headers, params, new TextHttpResponseHandler() {
            @Override
            public void onFailure(final int statusCode, Header[] headers, final String responseString, Throwable throwable) {
                Logger.error(TAG, "http request error, statusCode: %d, responseString: %s, url: %s, thread: %s",
                        statusCode, responseString, url, Thread.currentThread().getName(), throwable);
                callback.onFailure(url, statusCode, ResultCallback.EXCEPTION, throwable);
                report(startTime, url, Profiler.Status.ERROR.value);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                callback.onSuccess(url, statusCode, responseString);
                report(startTime, url, Profiler.Status.SUCCESS.value);
            }
        });

    }

    /**
     * use {@link #get(String, com.yy.androidlib.util.http.AsyncHttp.ResultCallback, org.apache.http.Header...)} instead
     *
     * @param url
     * @param callback
     * @param headers
     */
    @Deprecated
    public static void get(final String url, final Callback callback, final Header... headers) {
        get(url, transferToNewCallback(callback), headers);
    }

    private static void report(long startTime, String url, int status) {
        if (profiling != null) {
            profiling.report(startTime, url, status);
        }
    }

}
