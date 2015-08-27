//package com.yy.androidlib.util.db;
//
///**
// * Created by liukui on 14-7-10.
// */
//import java.lang.reflect.Field;
//import java.util.ArrayList;
//import java.util.List;
//
//import android.database.sqlite.SQLiteDatabase;
//import android.text.TextUtils;
//import android.util.SparseArray;
//import com.yy.androidlib.util.apache.StringUtils;
//import com.yy.androidlib.util.logging.Logger;
//
//public class ObjectTableCreater {
//
//    private static final char Space = ' ';
//    private static final String DIVIDER = ", ";
//    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS %s (%s)";
//    private static final String NOT_NULL = "not null";
//    private static final String PRIMARY = "primary key (%s)";
//    private static final String CREATE_INDEX = "CREATE INDEX %s_%s ON %s (%s)";
//
//    private String mTableName;
//    private List<String> mColumns = new ArrayList<String>();
//    private List<String> mPrimarys = new ArrayList<String>();
//    private SparseArray<List<String>> mIndexList = new SparseArray<List<String>>();
//
//    public boolean createObjectTable(Class<?> cls, SQLiteDatabase db) {
//        mColumns.clear();
//        mPrimarys.clear();
//        mIndexList.clear();
//
//        mTableName = cls.getSimpleName();
//        Field[] fields = cls.getDeclaredFields();
//        for (Field field : fields) {
//
//            DBColumn info = field.getAnnotation(DBColumn.class);
//            if (info != null) {
//                addColumn(field, info);
//                String name = field.getName();
//                addPrimary(name, info);
//                addIndex(name, info);
//            }
//        }
//        return createTable(db);
//    }
//
//    private boolean createTable(SQLiteDatabase db) {
//        String createTable = getCreateTableSql();
//        if (!StringUtils.isEmpty(createTable)) {
//            try {
//                Logger.debug(this, "success to create table %s", this.mTableName);
//                db.execSQL(createTable);
//
//                List<String> indexSqls = getCreateIndexSql();
//                if (!(indexSqls==null || indexSqls.isEmpty())) {
//                    for (String sql : indexSqls) {
//                        db.execSQL(sql);
//                    }
//                }
//                return true;
//            }
//            catch (Exception e) {
//                Logger.error(this, "create table %s failed, %s", mTableName, e);
//            }
//        }
//        return false;
//    }
//
//    public String getCreateTableSql() {
//        if (mColumns.isEmpty()) {
//            return null;
//        }
//        StringBuilder columns = new StringBuilder(TextUtils.join(DIVIDER, mColumns));
//        if (!mPrimarys.isEmpty()) {
//            columns.append(DIVIDER);
//            columns.append(String.format(PRIMARY, TextUtils.join(DIVIDER, mPrimarys)));
//        }
//        return String.format(CREATE_TABLE, mTableName, columns.toString());
//    }
//
//    public List<String> getCreateIndexSql() {
//        List<String> indexs = new ArrayList<String>();
//        for (int i = 0; i < mIndexList.size(); i++) {
//            List<String> indexColumns = mIndexList.valueAt(i);
//            String name = TextUtils.join("_", indexColumns);
//            String columns = TextUtils.join(DIVIDER, indexColumns);
//            indexs.add(String.format(CREATE_INDEX, mTableName, name, mTableName, columns));
//        }
//        return indexs;
//    }
//
//    private void addColumn(Field field, DBColumn info) {
//        String fieldName = field.getName();
//
//        StringBuilder column = new StringBuilder();
//        column.append(fieldName);
//        column.append(Space);
//        column.append(getFieldType(field));
//        if (info.isNotNull()) {
//            column.append(Space);
//            column.append(NOT_NULL);
//        }
//        mColumns.add(column.toString());
//    }
//
//    private void addPrimary(String name, DBColumn info) {
//        if (info.isPrimary() && !mPrimarys.contains(name)) {
//            mPrimarys.add(name);
//        }
//    }
//
//    private void addIndex(String name, DBColumn info) {
//        int[] indexType = info.indexType();
//        if (indexType != null && indexType.length > 0) {
//            for (int type : indexType) {
//                List<String> indexColumns = mIndexList.get(type);
//                if (indexColumns == null) {
//                    indexColumns = new ArrayList<String>();
//                    mIndexList.put(type, indexColumns);
//                }
//                if (!indexColumns.contains(name)) {
//                    indexColumns.add(name);
//                }
//            }
//        }
//    }
//
//    private String getFieldType(Field f) {
//        Class<?> type = f.getType();
//        if (type.equals(int.class) || type.equals(Integer.class)
//                || type.equals(Short.class) || type.equals(short.class)
//                || type.equals(char.class)) {
//            return "int";
//        }
//        else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
//            return "bool";
//        }
//        else if (type.equals(long.class) || type.equals(Long.class)) {
//            return "long";
//        }
//        else if (type.equals(float.class) || type.equals(Float.class)) {
//            return "float";
//        }
//        else {
//            return "text";
//        }
//    }
//}
