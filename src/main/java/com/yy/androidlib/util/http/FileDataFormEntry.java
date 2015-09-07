package com.yy.androidlib.util.http;

/**
 * User: lxl
 * Date: 10/29/14
 * Time: 10:00 AM
 */
public class FileDataFormEntry extends FormEntry {

    public byte[] byteValue;
    public FileDataFormEntry(String name, byte[] byteValue) {
        super(Type.FileData, name, "");
        this.byteValue = byteValue;
    }
}
