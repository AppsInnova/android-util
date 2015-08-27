package com.yy.androidlib.util.appinfo;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import com.yy.androidlib.util.logging.Logger;

import java.util.ArrayList;
import java.util.List;

public class AppInfoUtil {

    public static final String BAIDU_MARKET = "com.baidu.appsearch";
    public static final String AND_MARKET = "com.hiapk.marketpho";
    public static final String GFAN_MARKET = "com.mappn.gfan";
    public static final String QIHOO360_MARKET = "com.qihoo.appstore";
    public static final String TENCENT_MARKET = "com.tencent.android.qqdownloader";
    public static final String WANDUOJIA_MARKET = "com.wandoujia.phoenix2";
    public static final String XIAOMI_MARKET = "com.xiaomi.market";
    public static final String YINGYONGHUI_MARKET = "com.yingyonghui.market";

    public static String getAppVersion(Context context) {
        if (context == null) {
            return "";
        }
        PackageInfo packInfo = getPackageInfo(context);
        if (packInfo != null && packInfo.versionName != null) {
            if (packInfo.versionName.contains("SNAPSHOT")) {
                return packInfo.versionName.substring(0, packInfo.versionName.lastIndexOf('.')) + "." + getSvnBuildVersion(context);
            } else {
                return packInfo.versionName;
            }
        }
        return "";
    }

    public static boolean isSnapShot(Context context) {
        PackageInfo packInfo = getPackageInfo(context);
        return packInfo != null && packInfo.versionName.contains("SNAPSHOT");
    }

    private static PackageInfo getPackageInfo(Context context) {
        PackageInfo packInfo = null;
        try {
            PackageManager packageManager = context.getPackageManager();
            packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Logger.error("PackageManager.NameNotFoundException context", e);
        }
        return packInfo;
    }

    public static String getAndroidVersionRelease() {
        String versionRelease = "";
        try {
            versionRelease = android.os.Build.VERSION.RELEASE;
        } catch (NumberFormatException e) {
            Logger.error("NumberFormatException context", e);
        }
        return versionRelease;
    }

    public static String getAndroidModel() {
        String androidModel = "";
        try {
            androidModel = android.os.Build.MODEL;
        } catch (NumberFormatException e) {
            Logger.error("NumberFormatException context", e);
        }
        return androidModel;
    }

    public static int getSvnBuildVersion(Context context) {
        int svnBuildVer = 0;
        try {
            if (context != null) {
                String pkgName = context.getPackageName();
                ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(pkgName, PackageManager.GET_META_DATA);
                svnBuildVer = appInfo.metaData.getInt("SvnBuildVersion");
            }
        } catch (Exception e) {
            Logger.error(AppInfoUtil.class, e);
        }

        return svnBuildVer;
    }

    public static boolean isInstallMarketApp(Context context) {
        if (context == null) {
            return false;
        }
        List<PackageInfo> packageInfoList = getAllApps(context);
        for (PackageInfo packageInfo : packageInfoList) {
            if (packageInfo != null && packageInfo.applicationInfo != null) {
                String packageName = packageInfo.applicationInfo.packageName;
                if (packageName == null) {
                    continue;
                }
                if (packageName.equals(BAIDU_MARKET) || packageName.equals(AND_MARKET) || packageName.equals(GFAN_MARKET) || packageName.equals(QIHOO360_MARKET) || packageName.equals(TENCENT_MARKET) || packageName.equals(WANDUOJIA_MARKET) || packageName.equals(XIAOMI_MARKET) || packageName.equals(YINGYONGHUI_MARKET)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<PackageInfo> getAllApps(Context context) {
        List<PackageInfo> apps = new ArrayList<PackageInfo>();
        PackageManager pManager = context.getPackageManager();
        //获取手机内所有应用
        List<PackageInfo> paklist = pManager.getInstalledPackages(0);
        for (int i = 0; i < paklist.size(); i++) {
            PackageInfo pak = (PackageInfo) paklist.get(i);
            //判断是否为非系统预装的应用  
            apps.add(pak);
        }
        return apps;
    }

    /**
     * get apk release channel for Hiido statistics
     *
     * @param context
     * @return apk release channel id
     */
    public static String getHiidoChannelID(Context context) {
        String channelID = null;
        try {
            if (context != null) {
                String pkgName = context.getPackageName();
                ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(pkgName, PackageManager.GET_META_DATA);
                channelID = appInfo.metaData.getString("HIIDO_CHANNEL");
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
        channelID = channelID == null ? "" : channelID;
        return channelID;
    }
}
