package com.yy.androidlib.util.sdk;

import android.content.Context;
import android.telephony.TelephonyManager;

public class PhoneUtil {

    public static String getPhoneNumber(Context context) {
        try {
            TelephonyManager tMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String line1Number = tMgr.getLine1Number();
            if (line1Number == null) {
                line1Number = "";
            } else if (line1Number.startsWith("+86")) {
                line1Number = line1Number.substring(3, line1Number.length());
            }
            return line1Number;
        } catch (Exception e) {
            return "";
        }
    }
}
