//package com.yy.androidlib.util.db;
//
//import android.content.Context;
//import android.database.sqlite.SQLiteDatabase;
//import android.database.sqlite.SQLiteOpenHelper;
//import com.yy.androidlib.util.logging.Logger;
//
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
///**
// * Created by liukui on 14-7-10.
// */
//public class DBHelper extends SQLiteOpenHelper {
//
//    protected SQLiteDatabase mDb = null;
//    protected String mName;
//
//    private ExecutorService mThread;
//    private boolean mRunning;
//
//    public DBHelper(Context context, String name, int version) {
//        super(context, name, null, version);
//        mName = name;
//    }
//
//    public String getName() {
//        return mName;
//    }
//
//    public SQLiteDatabase getDb() {
//        synchronized (this) {
//            return mDb;
//        }
//    }
//
//    @Override
//    public void onCreate(SQLiteDatabase db) {
//    }
//
//    @Override
//    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//    }
//
//    public void postWriteTask(final Runnable r) {
//        Runnable writeTask = new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    r.run();
//                }
//                catch (Exception e) {
//                    Logger.error("dingning", "DB Error : WriteTask exception, %s", e);
//                }
//            }
//        };
//        synchronized (this) {
//            if (mThread == null || mThread.isShutdown()) {
//                mThread = Executors.newSingleThreadExecutor();
//            }
//            mThread.execute(writeTask);
//        }
//    }
//
//    public void start() {
//        synchronized (this) {
//            if (!mRunning) {
//                mRunning = true;
//                try{
//                    if (mDb == null) {
//                        mDb = this.getWritableDatabase();
//                    }
//                    else {
//                        Logger.info(this, "DB start: last db is still running");
//                    }
//                } catch(Exception e) { //SQLiteDatabaseLockedException
//                    Logger.error(this, "DB Error : start() Databases exception: %s", e);
//                    mRunning = false;
//                }
//            }
//        }
//    }
//
//    public boolean isRunning() {
//        synchronized (this) {
//            return mRunning;
//        }
//    }
//
//    private class DBStopTask implements Runnable {
//        private Runnable mStopTask;
//
//        public void setStopTask(Runnable stopTask) {
//            mStopTask = stopTask;
//        }
//
//        @Override
//        public void run() {
//            synchronized (this) {
//                if (!mRunning) {
//                    try {
//                        // beyond 4.0, endTransaction must be called before close db.
//                        if (mDb.inTransaction()) {
//                            try {
//                                mDb.setTransactionSuccessful();
//                                mDb.endTransaction();
//                            }
//                            catch (Throwable e) {
//                                Logger.error(this, "endTransaction for %s fail, %s", getName(), e);
//                            }
//                        }
//                        mDb.close();
//                        mDb = null;
//                    }
//                    catch (Throwable e) {
//                        Logger.warn(this, "Error happened during stoping DB %s, %s", getName(), e);
//                    }
//                    try {
//                        if (mStopTask != null) {
//                            mStopTask.run();
//                            mStopTask = null;
//                        }
//                    }
//                    catch (Throwable e) {
//                        Logger.warn(this, "Error happened in stop task: %s", mStopTask, e);
//                    }
//                    if (mThread != null && !mThread.isShutdown()) {
//                        mThread.shutdownNow();
//                        mThread = null;
//                    }
//                }
//                else {
//                    Logger.info(this, "DBStopTask, db is running again, cancel stop request");
//                }
//            }
//        }
//    }
//    private DBStopTask mDBStopTask = new DBStopTask();
//
//    public void stop(final Runnable stopTask) {
//        synchronized (this) {
//            if (mRunning) {
//                mRunning = false;
//                mDBStopTask.setStopTask(stopTask);
//                if (mThread != null && !mThread.isShutdown()) {
//                    mThread.execute(mDBStopTask);
//                }
//                else {
//                    mDBStopTask.run();
//                }
//            }
//        }
//    }
//
//}
