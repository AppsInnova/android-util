package com.yy.androidlib.util.http;

/**
 * Created by xuduo on 6/23/15.
 */
public interface Profiler {

    enum Status{

        SUCCESS(0),ERROR(-500),UNKNOW(1);

        public int value;

        Status(int code){
            this.value = code;
        }
    }

    void report(long startTime, String url, int status);

}
