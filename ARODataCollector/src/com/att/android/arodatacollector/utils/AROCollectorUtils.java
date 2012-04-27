/*
 * Copyright 2012 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.att.android.arodatacollector.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.os.StatFs;
import android.provider.Settings;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;


/**
 * Contains utility methods that are used by the ARO Data Collector. 
 * 
 */
public class AROCollectorUtils {

	/** Logging string for the ARO Collector Utils class. */
	public static final String TAG = "AROCollectorUtils";
	
	/**
	 * Gets the ARO Data Collector time stamp
	 * 
	 * @return The time stamp value in seconds.
	 */
	public double getDataCollectorEventTimeStamp() {
		return getSystemTimeinSeconds();
	}

	/**
	 * Gets the system time value(in seconds). 
	 * 
	 * @return The system time stamp value(in seconds ).
	 */
	public double getSystemTimeinSeconds() {
		return (double) System.currentTimeMillis()/1000;
	}
	/**
	 * Deletes the specified trace directory from the device.
	 *  
	 * @param path The trace directory path to be deleted.
	 * 
	 * @return A boolean value that is "true" if the trace folder was deleted successfully, and "false" if it was not.
	 */
	public boolean deleteDirectory(File path) {
		if (path.exists()) {
			final File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		
		return (path.delete());
	}

	/**
	 * Indicates whether or not there are special characters in the specified trace folder name.
	 * 
	 * @param tracefolername The trace folder name.
	 * 
	 * @return A boolean value that is "true" if the trace folder name contains a special character and "false" if it does not.
	 */
	public boolean isContainsSpecialCharacterorSpace(String tracefolername) {
		boolean isContainsSC = false;
		if (tracefolername != null && !tracefolername.equals("")) {
			// Pattern to include alphanumeric with "-"
			final Matcher m = Pattern.compile("[^a-zA-Z0-9[-]]").matcher(
					tracefolername);
			if (m.find()) {
				isContainsSC = true;
			} else {
				isContainsSC = false;
			}
		}
		return isContainsSC;
	}
	

	/**
	 * Returns a value that indicates whether or not the flight mode is turned on.
	 * 
	 * @param context The application context.
	 * @return A boolean value that is "true" if the flight mode is ON and "false" if it is not.
	 */
	public boolean isAirplaneModeOn(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) != 0;

    }

	/**
	 * Fetch the value of given field from the class dump specified using reflection.  
	 * 
	 * @param mClass The class name.
     * @param mInstance The object which contains the values.
     * @param fieldName The name of the specified field.
     * @return  The value of the specified  field from the class dump.
	 */
	public final String getSpecifiedFieldValues(Class<?> mClass,
			Object mInstance, String fieldName) {

		String fieldValue = "";
		
		if (mClass == null || mInstance == null || fieldName == null)
			return fieldValue;

		try {
			final Field field = mClass.getDeclaredField(fieldName);

			if (field != null) {
				field.setAccessible(true);
				fieldValue = field.get(mInstance).toString();
			}

		} catch (NoSuchFieldException exp) {
			fieldValue = "";
			Log.e(TAG,
					"Exception in getSpecifiedFieldValues NoSuchFieldException"
							+ exp);
		} catch (IllegalAccessException ile) {
			fieldValue = "";
			Log.e(TAG,
					"Exception in getSpecifiedFieldValues IllegalAccessException"
							+ ile);
		}

		return fieldValue;
	}
	/**
	 * Returns a value indicating whether or not the device SD card is mounted. 
	 * 
	 * @return A boolean value that is "true" if the SD card is mounted, and "false" if it is not.
	 */
	public boolean checkSDCardMounted(){
		final String state = Environment.getExternalStorageState();
		if (state.equals(Environment.MEDIA_REMOVED) || !state.equals(Environment.MEDIA_MOUNTED) 
				|| state.equals(Environment.MEDIA_MOUNTED_READ_ONLY) ) {
			return true;
		}
		else{
			return false;
		}
	}
	
	/**
	 * Executes the specified linux command on the device shell. 
	 * 
	 * @param shellCommand A linux native shell command.
	 * 
	 * @return The output from the linux native shell command.
	 */
	public String runCommand(String shellCommand) {
		String stdout;
		String sRet = "";
		String stderr;
		try {
			final Process m_process = Runtime.getRuntime().exec(shellCommand);
			final StringBuilder sbread = new StringBuilder();
			final Thread tout = new Thread(new Runnable() {
				public void run() {
					BufferedReader bufferedReader = new BufferedReader(
							new InputStreamReader(m_process.getInputStream()),8192);
					String ls_1 = null;
					try {
						while ((ls_1 = bufferedReader.readLine()) != null) {
							sbread.append(ls_1).append("\n");
						}
					} catch (IOException e) {
						Log.e(TAG,"IOException in runCommand"+e);
					} finally {
						try {
							bufferedReader.close();
						} catch (IOException e) {
							Log.e(TAG,"Exception in runCommand bufferedReader.close()"+e);
						}
					}
				}
			});
			tout.start();
			final StringBuilder sberr = new StringBuilder();
			final Thread terr = new Thread(new Runnable() {
				public void run() {
					final BufferedReader bufferedReader = new BufferedReader(
							new InputStreamReader(m_process.getErrorStream()),
							8192);
					String ls_1 = null;
					try {
						while ((ls_1 = bufferedReader.readLine()) != null) {
							sberr.append(ls_1).append("\n");
						}
					} catch (IOException e) {
						Log.e(TAG,"Exception in runCommand"+e);
					} finally {
						try {
							bufferedReader.close();
						} catch (IOException e) {
							Log.e(TAG,"Exception in runCommand bufferedReader.close()"+e);
						}
					}
				}
			});
			terr.start();
			while (tout.isAlive()) {
				Thread.sleep(50);
			}
			if (terr.isAlive())
				terr.interrupt();
			stdout = sbread.toString();
			stderr = sberr.toString();
			sRet = stdout + stderr;
		} catch (java.io.IOException ee) {
			Log.e(TAG,"Exception in runCommand"+ee);
			return null;
		} catch (InterruptedException ie) {
			Log.e(TAG,"Exception in runCommand"+ie);
			return null;
		} 
		return sRet;
	}

	/**
	 * Gets the process ID for the specified process name.
	 * 
	 * @param processName The name of the process.
	 * 
	 * @return The process ID.
	 * @throws java.io.IOException 
	 * @throws java.lang.InterruptedException 
	 * @throws java.lang.IndexOutOfBoundsException
	 */
	public int getProcessID(String processName) throws IOException,IndexOutOfBoundsException,
			InterruptedException {
		
		final Process process = Runtime.getRuntime().exec("ps " + processName);
		final InputStreamReader inputStream = new InputStreamReader(
				process.getInputStream());
		final BufferedReader reader = new BufferedReader(inputStream);
		try {
			String line = null;
			int pid = 0;
			int read;
			final char[] buffer = new char[4096];
			final StringBuffer output = new StringBuffer();
			while ((read = reader.read(buffer)) > 0) {
				output.append(buffer, 0, read);
			}
			// Waits for the command to finish.
			process.waitFor();
			process.destroy();
			line = output.toString();
			String[] values = line.split("\\n");
			line = null;
			line = values[1];
			values = line.split(" ");
			for (int i = 1; i < values.length; i++) {
				if (values[i].length() > 0) {
					String temp = null;
					temp = values[i];
					pid = Integer.valueOf(temp);
					return pid;
				}
			}
			return pid;
		} finally {
			reader.close();
			inputStream.close();
			reader.close();
		}
	}
	/**
	 * Gets the default ARO trace folder name in the HH:MM:SS:DD:MM:YY format.
	 * 
	 * @return The default ARO trace folder name.
	 */
	public String getDefaultTraceFolderName() {
		final Date systemDate = new Date();
		final Calendar now = Calendar.getInstance();
		final int currenthours = systemDate.getHours();
		final int currentminutes = systemDate.getMinutes();
		final int currentseconds = systemDate.getSeconds();
		final int currentdate = now.get(Calendar.DATE); // java calendar
		int currentmonth = now.get(Calendar.MONTH); // As Jan is defined as 0 in
		currentmonth = currentmonth + 1;
		if (currentmonth >= 13) // As Jan is defined as 0 in java calendar
			currentmonth = 1;
		String currentMonth = Integer.toString(currentmonth);
		String currentDate = Integer.toString(currentdate);
		String currentHours = Integer.toString(currenthours);
		String currentMinutes = Integer.toString(currentminutes);
		String currentSeconds = Integer.toString(currentseconds - 1);

		if (currentmonth < 10) {
			currentMonth = "";
			currentMonth = "0" + currentmonth;
		}
		if (currentdate < 10) {
			currentDate = "";
			currentDate = "0" + currentdate;
		}

		if (currenthours < 10) {
			currentHours = "";
			currentHours = "0" + currenthours;
		}
		if (currentminutes < 10) {
			currentMinutes = "";
			currentMinutes = "0" + currentminutes;
		}
		if (currentseconds < 10) {
			currentSeconds = "";
			currentSeconds = "0" + currentseconds;
		}
		final String folderName = now.get(Calendar.YEAR) + "-" + currentMonth + "-"
				+ currentDate + "-" + currentHours + "-" + currentMinutes + "-"
				+ currentSeconds;
		
		return folderName;
	}

	/**
	 * Retrieves overall information about the specified application package 
	 * 
	 * @param pm The package manager instance. 
	 * @param name The package name.
	 * 
	 * @return Returns information about the package or null if the package could not be successfully parsed.

	 */
	public static PackageInfo getPackageInfo(PackageManager pm, String name) {
		PackageInfo ret = null;
		try {
			ret = pm.getPackageInfo(name, PackageManager.GET_ACTIVITIES);
		} catch (NameNotFoundException e) {
			Log.e(TAG,"Exception in getPackageInfo"+e);
		}
		return ret;
	}

	/**
	 * Reverts the specified source string.
	 * 
	 * @param from The source string.
	 * @return The reverted string.
	 */
	public static String revert(String from) {
		if (from == null || from.length() <= 1)
			return from;
		final int len = from.length();
		final StringBuilder sb = new StringBuilder(len);
		for (int i = 1; i <= len; i++) {
			sb.append(from.charAt(len - i));
		}
		return sb.toString();
	}


	
	/**
	 * Parses the integer value of the specified object.
	 * 
	 * @param s The source object.
	 * @return An integer value parsed from the specified object.
	 */
	public static int parseInt(Object s) {
		return parseInt(s == null ? null : s.toString(), 0);
	}

	
	/**
	 * Parses the integer value of the specified string.
	 * 
	 * @param s The string value to parse.
	 * @param iDefault A default integer value.
	 * 
	 * @return An integer value parsed from the specified string.
	 */
	public static int parseInt(String s, int iDefault) {
		return (int) parseLong(s, iDefault);
	}
	
	/**
	 * Parses the long value of the specified string.
	 * 
	 * @param s The string value to parse.
	 * @param iDefault A default long value. 
	 * @return A long value parsed from the specified string.
	 */
	public static long parseLong(String s, long iDefault) {
		if (s == null || s.equals(""))
			return iDefault;
		try {
			s = s.trim().replaceAll(",", "");
			final int l = s.indexOf('.');
			if (l > 0)
				s = s.substring(0, l);
			return Long.parseLong(s);
		} catch (RuntimeException e) {
			return iDefault;
		}
	}
	

	/**
	 * Returns the IP Address of the device during the trace cycle when it is connected to the network.
	 * 
	 * @return The IP Address of the device.
	 * 
	 * @throws java.net.SocketException 
	 */
	public String getLocalIpAddress() throws SocketException {
			for (final Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				final NetworkInterface intf = en.nextElement();
				for (final Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					final InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress();
					}
				}
			}
		return null;
	}
	
	/**
	 * Deletes the specified trace folder. 
	 * 
	 * @param tracefoldername The name of the trace folder to be deleted.
	 * @return A boolean value that is "true" if the trace folder was successfully deleted and "false" if it was not.
	 */
	public boolean deleteTraceFolder(File tracefoldername) {
		
		if (tracefoldername.isDirectory()) {
			final String[] children = tracefoldername.list();
			for (int i = 0; i < children.length; i++) {
				if (!deleteTraceFolder(new File(tracefoldername, children[i]))) {
					return false;
				}
			}
		}
		// The directory is now empty so delete it
		return tracefoldername.delete();
	}
	/**
	 * Returns the amount of available memory left on the device SD Card (in bytes).
	 */
	public long checkSDCardMemoryAvailable() {
	    final StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
	    final long bytesAvailable = (long)stat.getBlockSize() * (long)stat.getAvailableBlocks();
	    final long kbsAvailable = bytesAvailable /1024;
	   // float megAvailable =  (long) (bytesAvailable / (1024.f * 1024.f));
	    return kbsAvailable;
	}
	
	/**
	 * Opens the http connection to http://www.google.com/, and enables the network data packet.
	 * 
	 * @throws java.io.IOException 
	 * @throws ClientProtocolException 
	 */
	public void OpenHttpConnection() throws ClientProtocolException,IOException {
		
		final String strUrl = "http://www.google.com";
		final int timeoutConnection = 15000;
		final int timeoutSocket = 15000;
		InputStream in = null;
		BufferedReader reader = null;
		InputStreamReader inputstreamReader = null;
		HttpClient client = null;
		HttpResponse response = null;
		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters,timeoutConnection);
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
		client = new DefaultHttpClient(httpParameters);
		HttpPost post = new HttpPost(strUrl);
		try {
			// add headers
			post.addHeader("Content-Type","application/x-www-form-urlencoded; charset=utf-8");
			post.addHeader("Cache-Control", "no-cache");
			post.addHeader("Pragma", "no-cache");
			response = client.execute(post);
			in = response.getEntity().getContent();
			inputstreamReader =new InputStreamReader(in);
			reader = new BufferedReader(inputstreamReader);
			final StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line + "\n");
			}

		} finally {
			if (inputstreamReader != null) {
				inputstreamReader.close();
			}
			if (in != null) {
				in.close();
			}
			if (reader != null) {
				reader.close();
			}
			if (client != null) {
				client.getConnectionManager().shutdown();
			}
			httpParameters = null;
			post = null;
			client = null;
			response = null;

		}
	}
}
