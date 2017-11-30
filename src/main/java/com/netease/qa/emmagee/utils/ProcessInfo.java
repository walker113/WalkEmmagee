/*
 * Copyright (c) 2012-2013 NetEase, Inc. and other contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.netease.qa.emmagee.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.socks.library.KLog;

/**
 * get information of processes
 * 
 * @author andrewleo
 */
public class ProcessInfo {

	private static final String LOG_TAG = "Emmagee-"
			+ ProcessInfo.class.getSimpleName();

	private static final String PACKAGE_NAME = "com.netease.qa.emmagee";
	private static final int ANDROID_M = 22;

	/**
	 * get information of all running processes,including package name ,process
	 * name ,icon ,pid and uid.
	 * 
	 * @param context
	 *            context of activity
	 * @return running processes list
	 */
	public List<Programe> getRunningProcess(Context context) {
		Log.i(LOG_TAG, "get running processes");
		List<Programe> progressList = new ArrayList<Programe>();
		PackageManager pm = context.getPackageManager();

		ActivityManager am = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		// 通过am的getRunningAppProcesses方法获取到运行的进程信息
		List<RunningAppProcessInfo> run = am.getRunningAppProcesses();
		//遍历进程的列表将进程名，进程id赋值到program对象上
		for (ApplicationInfo appinfo : getPackagesInfo(context)) {
			Programe programe = new Programe();
			if (((appinfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0)
					|| ((appinfo.processName != null) && (appinfo.processName
							.equals(PACKAGE_NAME)))) {
				continue;
			}
			// 通过比较我们选择的应用的包名以及后台运行的进程的包名来判断app是否已经正常运行起来了
			for (RunningAppProcessInfo runningProcess : run) {
				if ((runningProcess.processName != null)
						&& runningProcess.processName
								.equals(appinfo.processName)) {
					programe.setPid(runningProcess.pid);
					programe.setUid(runningProcess.uid);
					break;
				}
			}
			programe.setPackageName(appinfo.processName);
			programe.setProcessName(appinfo.loadLabel(pm).toString());
			programe.setIcon(appinfo.loadIcon(pm));
			progressList.add(programe);
		}
		Collections.sort(progressList);
		return progressList;
	}

	/**
	 * 运行进程包含自己
	 * @param context
	 * @return
	 */
	public Programe getRunningProcessSelf(Context context) {
		Log.i(LOG_TAG, "get running processes");
		PackageManager pm = context.getPackageManager();

		ActivityManager am = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		// 通过am的getRunningAppProcesses方法获取到运行的进程信息
		List<RunningAppProcessInfo> run = am.getRunningAppProcesses();
		//遍历进程的列表将进程名，进程id赋值到program对象上
		for (ApplicationInfo appinfo : getPackagesInfo(context)) {
			Programe programe = new Programe();
			if (appinfo.processName.equals(PACKAGE_NAME)) {
				programe.setPackageName(appinfo.processName);
				programe.setProcessName(appinfo.loadLabel(pm).toString());
				programe.setIcon(appinfo.loadIcon(pm));
				return programe;
			}
		}
		return null;
	}

	/**
	 * get pid by package name
	 * 
	 * @param context
	 *            context of activity
	 * @return pid
	 */
	public int getPidByPackageName(Context context, String packageName) {
		Log.i(LOG_TAG, "start getLaunchedPid");
		ActivityManager am = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		// Note: getRunningAppProcesses return itself in API 22
		if (Build.VERSION.SDK_INT < ANDROID_M) {
			List<RunningAppProcessInfo> run = am.getRunningAppProcesses();
			for (RunningAppProcessInfo runningProcess : run) {
				if ((runningProcess.processName != null)
						&& runningProcess.processName.equals(packageName)) {
					return runningProcess.pid;
				}
			}
		} else {
			Log.i(LOG_TAG, "use top command to get pid");
			try {
				Process p = Runtime.getRuntime().exec("top -m 100 -n 1");
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
						p.getInputStream()));
				String line = "";
				while ((line = bufferedReader.readLine()) != null) {
					if (line.contains(packageName)) {
						line = line.trim();
						String[] splitLine = line.split("\\s+");
						if (packageName.equals(splitLine[splitLine.length - 1])) {
							return Integer.parseInt(splitLine[0]);
						}
					}
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		return 0;
	}

	/**
	 * get information of all installed packages
	 * 
	 * @param context
	 *            context of activity
	 * @return all installed packages
	 */
	public List<Programe> getAllPackages(Context context) {
		Log.i(LOG_TAG, "getAllPackages");
		List<Programe> progressList = new ArrayList<Programe>();
		PackageManager pm = context.getPackageManager();

		for (ApplicationInfo appinfo : getPackagesInfo(context)) {
			Programe programe = new Programe();
			if (((appinfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0)
					|| ((appinfo.processName != null) && (appinfo.processName
							.equals(PACKAGE_NAME)))) {
				continue;
			}
			programe.setPackageName(appinfo.processName);
			programe.setProcessName(appinfo.loadLabel(pm).toString());
			programe.setIcon(appinfo.loadIcon(pm));
			progressList.add(programe);
		}
		Collections.sort(progressList);
		return progressList;
	}

	/**
	 * get information of all applications.
	 * 
	 * @param context
	 *            context of activity
	 * @return packages information of all applications
	 */
	private List<ApplicationInfo> getPackagesInfo(Context context) {
		PackageManager pm = context.getApplicationContext().getPackageManager();
		List<ApplicationInfo> appList = pm
				.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
		return appList;
	}

	/**
	 * get pid by package name
	 * 
	 * @param context
	 *            context of activity
	 * @param packageName
	 *            package name of monitoring app
	 * @return pid
	 */
	public Programe getProgrameByPackageName(Context context, String packageName) {
		if (Build.VERSION.SDK_INT < ANDROID_M) {
			List<Programe> processList = getRunningProcess(context);
			KLog.e(packageName);
			for (Programe programe : processList) {
				KLog.e(">> " + programe.getPackageName());
				if ((programe.getPackageName() != null)
						&& (programe.getPackageName().equals(packageName))) {
					return programe;
				}
			}
		} else {
			Programe programe = new Programe();
			int pid = getPidByPackageName(context, packageName);
			programe.setPid(pid);
			programe.setUid(0);
			return programe;
		}
		return null;
	}

	/**
	 * get top activity name
	 * 
	 * @param context
	 *            context of activity
	 * @return top activity name
	 */
	public static String getTopActivity(Context context) {
		ActivityManager manager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		// Note: getRunningTasks is deprecated in API 21(Official)
		if (Build.VERSION.SDK_INT >= 21) {
			return Constants.NA;
		}
		List<RunningTaskInfo> runningTaskInfos = manager.getRunningTasks(1);
		if (runningTaskInfos != null)
			return (runningTaskInfos.get(0).topActivity).toString();
		else
			return null;
	}
}
