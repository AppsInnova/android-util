//package com.yy.androidlib.util.db;
//
///**
// * Created by liukui on 14-7-10.
// */
//import java.util.ArrayList;
//import java.util.List;
//
//import android.content.Context;
//import android.database.sqlite.SQLiteDatabase;
//
//
///**
// *
// * An ObjectDBHelper can manage one or more tables,
// * one table is created through a class type.
// *
// */
//public class ObjectDBHelper extends DBHelper {
//
//    private List<Class<?>> mClsList;
//
//    public ObjectDBHelper(Context context,String name, int version) {
//        super(context,name, version);
//    }
//
//    public void setObjectTableClass(Class<?> ... classes) {
//        mClsList = new ArrayList<Class<?>>();
//        for (Class<?> e : classes) {
//            mClsList.add(e);
//        }
//    }
//
//    public void setObjectTableClass(List<Class<?>> list) {
//        mClsList = list;
//    }
//
//    @Override
//    public void onCreate(SQLiteDatabase db) {
//        synchronized (this) {
//            if (!(mClsList==null || mClsList.isEmpty())) {
//                ObjectTableCreater tc = new ObjectTableCreater();
//                for (Class<?> cls : mClsList) {
//                    tc.createObjectTable(cls, db);
//                }
//            }
//        }
//    }
//
//}
