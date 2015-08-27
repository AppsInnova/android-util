package com.yy.androidlib.util.apache;

import android.content.Context;

import java.io.File;

public class FileUtils {
    public static String getDirOfFilePath(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            return null;
        }
        int sepPos = filePath.lastIndexOf(File.separatorChar);
        if (sepPos == -1) {
            return null;
        }
        return filePath.substring(0, sepPos);
    }

    public static String getFileName(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            return null;
        }
        int sepPos = filePath.lastIndexOf(File.separator) + 1;
        return filePath.substring(sepPos);
    }

    public static String getFileExtension(String filePath) {
        String fileName = getFileName(filePath);
        int index = fileName.lastIndexOf('.');
        if (index != -1) {
            return fileName.substring(index);
        }
        return null;
    }

    public static String getTempDir(Context context) {
        return context.getCacheDir().getParent();
    }
}
