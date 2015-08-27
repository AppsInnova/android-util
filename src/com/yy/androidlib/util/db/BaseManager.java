//package com.yy.androidlib.util.db;
//
///**
// * Created by liukui on 14-7-10.
// */
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.atomic.AtomicBoolean;
//
//import android.database.sqlite.SQLiteDatabase;
//import com.yy.androidlib.util.logging.Logger;
//
//
//public abstract class BaseManager {
//
//    protected static HashSet<BaseManager> sManagerSet = new HashSet<BaseManager>();
//    protected static ConcurrentHashMap<String, DBHelper> sDbMap = new ConcurrentHashMap<String, DBHelper>();
//
//    public static boolean hasRunningDb() {
//        return sDbMap.size() > 0;
//    }
//
//    public class DBInfo {
//        public String name;
//        public boolean share;
//
//        public DBInfo(String name, boolean share) {
//            this.name = name;
//            this.share = share;
//        }
//    }
//
//    public static void stopAllManagers() {
//        Set<BaseManager> mgrs = getManagerSet();
//        for (BaseManager mgr : mgrs) {
//            mgr.stopAll();
//        }
//    }
//
//    private static void recordManager(BaseManager m) {
//        synchronized (sManagerSet) {
//            sManagerSet.add(m);
//        }
//    }
//
//    private static void removeManager(BaseManager m) {
//        synchronized (sManagerSet) {
//            sManagerSet.remove(m);
//            if (sManagerSet.isEmpty()) {
//                Logger.info("DB", "all data manager closed");
//            }
//        }
//    }
//
//    private static boolean isManagerExist(BaseManager m) {
//        synchronized (sManagerSet) {
//            return sManagerSet.contains(m);
//        }
//    }
//
//    private static Set<BaseManager> getManagerSet() {
//        synchronized (sManagerSet) {
//            return new HashSet<BaseManager>(sManagerSet);
//        }
//    }
//
//    private AtomicBoolean mMgrStart = new AtomicBoolean(false);
//
//    public synchronized void start() { //和stop不能同时运行（也许导致db lock exception），所以加锁
//        if (mMgrStart.compareAndSet(false, true)) {
//            recordManager(this);
//            List<DBInfo> dbList = getDBInfoList();
//            if (dbList != null) {
//                for (DBInfo info : dbList) {
//                    startDBHelper(info);
//                }
//            }
//        }
//    }
//
//    public boolean isStarted() {
//        return mMgrStart.get();
//    }
//
//    private class StopTask implements Runnable {
//
//        private DBInfo mInfo;
//
//        public StopTask(DBInfo info) {
//            mInfo = info;
//        }
//
//        @Override
//        public void run() {
//            sDbMap.remove(mInfo.name);
//        }
//    };
//
//    public synchronized void stop() { //和start不能同时运行（也许导致db lock exception），所以加锁
//        if (mMgrStart.compareAndSet(true, false)) {
//            List<DBInfo> dbList = getDBInfoList();
//            if (dbList != null) {
//                for (DBInfo info : dbList) {
//                    if (!info.share) {
//                        stopDBHelper(info);
//                    }
//                }
//            }
//        }
//    }
//
//    public synchronized void stopAll() {
//        mMgrStart.set(false);
//        List<DBInfo> dbList = getDBInfoList();
//        if (dbList != null) {
//            for (DBInfo info : dbList) {
//                stopDBHelper(info);
//            }
//        }
//        removeManager(this);
//    }
//
//    private void stopDBHelper(DBInfo info) {
//        stopDBHelper(sDbMap.get(info.name), new StopTask(info));
//    }
//
//    private static void stopDBHelper(DBHelper helper, Runnable task) {
//        if (helper != null) {
//            helper.stop(task);
//        }
//    }
//
//    @SuppressWarnings("unused")
//    private String getCurrentDbMap() {
//        StringBuilder log = new StringBuilder();
//        log.append("[");
//        for (String key : sDbMap.keySet()) {
//            log.append(key);
//            log.append(", ");
//        }
//        log.append("]");
//        return log.toString();
//    }
//
//    protected synchronized SQLiteDatabase getDatabase(String name) {
//        if (mMgrStart.get()) {
//            DBHelper helper = getDb(name);
//            if (helper != null) {
//                return helper.getDb();
//            }
//        }
//        Logger.error(this, "get db %s failed for it isn't created", name);
//        return null;
//    }
//
//    private static void stopUnusedDb() {
//        Set<String> validList = new HashSet<String>();
//        Set<BaseManager> mgrSet = getManagerSet();
//        for (BaseManager mgr : mgrSet) {
//            List<DBInfo> dbList = mgr.getDBInfoList();
//            if (dbList != null) {
//                for (DBInfo db : dbList) {
//                    validList.add(db.name);
//                }
//            }
//        }
//        Map<String, DBHelper> unused = new HashMap<String, DBHelper>();
//        for (Entry<String, DBHelper> entry : sDbMap.entrySet()) {
//            if (!validList.contains(entry.getKey())) {
//                unused.put(entry.getKey(), entry.getValue());
//            }
//        }
//        for (Entry<String, DBHelper> entry : unused.entrySet()) {
//            sDbMap.remove(entry.getKey());
//            stopDBHelper(entry.getValue(), null);
//        }
//    }
//
//    private void startDBHelper(DBInfo db) {
//        if (db != null && db.name != null) {
//            DBHelper helper = sDbMap.get(db.name);
//            if (helper == null) {
//                synchronized (this) {
//                    helper = sDbMap.get(db.name);
//                    if (helper == null) {
//                        helper = createDb(db.name);
//                        if (helper != null) {
//                            sDbMap.put(db.name, helper);
//                        }
//                    }
//                }
//            }
//            if (helper != null) {
//                helper.start();
//            }
//        }
//    }
//
//    DBHelper getDb(String name) {
//        DBHelper helper = sDbMap.get(name);
//        if (helper == null) {
//            if (isManagerExist(this)) {
//                Logger.warn(this, "Manager is inconsistent, stop unused dbs and restart it");
//                stopUnusedDb();
//            }
//            mMgrStart.set(false);
//            start();
//            helper = sDbMap.get(name);
//        }
//        return helper;
//    }
//
//    protected void postWriteTask(String dbName, Runnable r) {
//        DBHelper helper = getDb(dbName);
//        if (helper != null) {
//            helper.postWriteTask(r);
//        }
//    }
//
//    protected abstract DBHelper createDb(String name);
//
//    /**
//     * All the related DBHelpers will be created by the DBInfo list.
//     * One DBHelper corresponds to one DBInfo.
//     */
//    protected abstract List<DBInfo> getDBInfoList();
//
//}
