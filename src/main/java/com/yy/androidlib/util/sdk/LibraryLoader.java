package com.yy.androidlib.util.sdk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.content.Context;
import android.os.Build;
import android.util.Log;

public class LibraryLoader {
	private static final String TAG = "LibraryLoader";

	public static boolean load(Context context, String libName, ClassLoader classLoader) {
		File nativePathFile = new File(context.getApplicationInfo().nativeLibraryDir);
		String libPath = null;

		if ("armeabi-v7a".equals(Build.CPU_ABI)) {
			File libFilev7a = new File(nativePathFile, "lib" + libName + "-v7a" + ".so");
			if (libFilev7a.exists()) {
				File libFileRecover = new File(context.getDir("lib_v7a", 0), "lib" + libName + ".so");
				if (!libFileRecover.exists() || libFilev7a.length() != libFileRecover.length()) {
					copy(libFilev7a, libFileRecover);
				}
				if (libFileRecover.exists() && libFileRecover.length() == libFilev7a.length()) {
					libPath = libFileRecover.getAbsolutePath();
				}
			}
		}

		if (libPath == null) {
			File libFile = new File(nativePathFile, "lib" + libName + ".so");
			if (libFile.exists()) {
				libPath = libFile.getAbsolutePath();
			}
		}

		if (libPath != null && load(libPath, classLoader)) {
			Log.i(TAG, libPath + " loaded");
			return true;
        } else if (loadLibrary(libName, classLoader)) {
			Log.i(TAG, libName + " loaded");
			return true;
		}

		ZipFile apkZipFile = null;
		try {
			File libFile = new File(context.getDir("lib_ext", 0), "lib" + libName + ".so");
			File apkFile = new File(context.getApplicationInfo().sourceDir);
			apkZipFile = new ZipFile(apkFile);
			if (copy(apkZipFile, libName, libFile)) {
				if (libFile.length() > 0 && load(libFile.getAbsolutePath(), classLoader)) {
					Log.i(TAG, libFile.getAbsolutePath() + " loaded");
					return true;
		        }
			}
		} catch (Throwable t) {
		} finally {
			try {
				if (apkZipFile != null) {
					apkZipFile.close();
				}
			} catch (Throwable t) {
			}
		}

		Log.e(TAG, libName + " load failed");
		return false;
	}

	private static boolean load(String libPath, ClassLoader classLoader) {
		try {
			Runtime rt = Runtime.getRuntime();
			Method method = rt.getClass().getDeclaredMethod("load", new Class[] { String.class, ClassLoader.class });
			method.setAccessible(true);
			method.invoke(rt, new Object[] { libPath, classLoader });
		} catch (InvocationTargetException e) {
			Log.e(TAG, "Fail to load library", e.getTargetException());
			return false;
		} catch (Throwable t) {
			Log.e(TAG, "Fail to load library", t);
			return false;
		}

		return true;
	}

    public static boolean loadLibrary(Context context, String libName) {
        Log.i("LibraryLoader", String.format("load library: %s", libName));
        try {
            System.loadLibrary(libName);
            Log.i("LibraryLoader", String.format("load library: %s success", libName));

            return true;
        }
        catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }

        boolean ret = load(context, libName, context.getClassLoader());
        if (ret) {
            Log.i("LibraryLoader", String.format("load library: %s success", libName));
        }
        else {
            Log.i("LibraryLoader", String.format("load library: %s failed", libName));
        }

        return ret;
    }

	private static boolean loadLibrary(String libName, ClassLoader classLoader) {
		try {
			Runtime rt = Runtime.getRuntime();
			Method method = rt.getClass().getDeclaredMethod("loadLibrary", new Class[] { String.class, ClassLoader.class });
			method.setAccessible(true);
			method.invoke(rt, new Object[] { libName, classLoader });
		} catch (InvocationTargetException e) {
			Log.e(TAG, "Fail to load library", e.getTargetException());
			return false;
		} catch (Throwable t) {
			Log.e(TAG, "Fail to load library", t);
			return false;
		}

		return true;
	}

	private static void copy(File oldfile, File newFile) {
		InputStream is = null;
		FileOutputStream os = null;
		try {
			if (newFile.exists()) {
				newFile.delete();
			}
			if (oldfile.exists()) {
				byte[] buffer = new byte[2048];
				int byteread = 0;
				is = new FileInputStream(oldfile);
				os = new FileOutputStream(newFile);
				while ((byteread = is.read(buffer)) != -1) {
					os.write(buffer, 0, byteread);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Fail to load library", e);
			newFile.delete();
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (Exception e) {
				Log.e(TAG, "Fail to load library", e);
			}
			try {
				if (os != null) {
					os.close();
				}
			} catch (Exception e) {
				Log.e(TAG, "Fail to load library", e);
			}
		}
	}
	
	private static boolean copy(ZipFile apkZipFile, String libName, File saveFile) {
		ZipEntry libEntry = null;

		if ("armeabi-v7a".equals(Build.CPU_ABI)) {
			libEntry = apkZipFile.getEntry("lib/armeabi-v7a/lib" + libName + ".so");
			if (libEntry == null) {
				libEntry = apkZipFile.getEntry("lib/armeabi/lib" + libName + "-v7a.so");
			}
		}
		if (libEntry == null) {
			libEntry = apkZipFile.getEntry("lib/armeabi/lib" + libName + ".so");
		}
		if (libEntry == null) {
			return false;
		}
		if (libEntry.getSize() == saveFile.length()) {
			return true;
		}
		if (saveFile.exists()) {
			saveFile.delete();
		}

		InputStream is = null;
		FileOutputStream os = null;
		try {
			is = apkZipFile.getInputStream(libEntry);
			os = new FileOutputStream(saveFile);
			byte[] buffer = new byte[2048];
			int byteread = 0;
			while ((byteread = is.read(buffer)) != -1) {
				os.write(buffer, 0, byteread);
			}

			return true;
		} catch (Throwable t) {
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (Exception e) {
				Log.e(TAG, "Fail to load library", e);
			}
			try {
				if (os != null) {
					os.close();
				}
			} catch (Exception e) {
				Log.e(TAG, "Fail to load library", e);
			}
		}

		return false;
	}
}
