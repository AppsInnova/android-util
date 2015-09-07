package com.yy.androidlib.util.date;

import java.util.Date;

public class TimeUtils {

    public static final long MS_PER_MINUTE = 60 * 1000;

    public static Date minuteBefore(int minute) {
        return new Date(System.currentTimeMillis() - minute * MS_PER_MINUTE);
    }
}
