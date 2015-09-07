package com.yy.androidlib.util.http;

/**
 * User: lxl
 * Date: 8/22/14
 * Time: 3:29 PM
 */
public class FormEntry {

    public static enum Type {
        String, File, ZipData, ZipFile, FileBlock, FileData
    }

    public Type type;
    public String name;
    public String value;
    public int startPos;
    public int size;
    public int index;

    public FormEntry(Type t, String n, String v) {
        type = t;
        name = n;
        value = v;
    }
}
