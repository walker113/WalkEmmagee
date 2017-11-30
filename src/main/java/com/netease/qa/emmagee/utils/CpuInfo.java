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

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Pattern;

import com.netease.qa.emmagee.R;
import com.netease.qa.emmagee.service.EmmageeService;

import android.content.Context;
import android.os.Build;
import android.util.Log;

/**
 * operate CPU information
 * 
 * @author andrewleo
 */
public class CpuInfo {

	private static final String LOG_TAG = "Emmagee-" + CpuInfo.class.getSimpleName();

	private Context context;
	private long processCpu;
	private ArrayList<Long> idleCpu = new ArrayList<Long>();
	private ArrayList<Long> totalCpu = new ArrayList<Long>();
	private boolean isInitialStatics = true;
	private SimpleDateFormat formatterFile;
	private MemoryInfo mi;
	private long totalMemorySize;
	private long preTraffic;
	private long lastestTraffic;
	private long traffic;
	private TrafficInfo trafficInfo;
	private ArrayList<String> cpuUsedRatio = new ArrayList<String>();
	private ArrayList<Long> totalCpu2 = new ArrayList<Long>();
	private long processCpu2;
	private ArrayList<Long> idleCpu2 = new ArrayList<Long>();
	private String processCpuRatio = "";
	private ArrayList<String> totalCpuRatio = new ArrayList<String>();
	private int pid;

	private static final String INTEL_CPU_NAME = "model name";
	private static final String CPU_DIR_PATH = "/sys/devices/system/cpu/";
	private static final String CPU_X86 = "x86";
	private static final String CPU_INFO_PATH = "/proc/cpuinfo";
	private static final String CPU_STAT = "/proc/stat";

	public CpuInfo(Context context, int pid, String uid) {
		this.pid = pid;
		this.context = context;
		trafficInfo = new TrafficInfo(uid);
		formatterFile = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		mi = new MemoryInfo();
		totalMemorySize = mi.getTotalMemory();
		cpuUsedRatio = new ArrayList<String>();
	}

	/**
	 * read the status of CPU.
	 * 
	 * @throws FileNotFoundException
	 */
	public void readCpuStat() {
		/*
		 * TODO :　当前进程cpu的占用率
		 * android设备对应的pid的CPU使用情况实在/proc/pid/stat路径下
		 * 文本的内容为
		 * 9978 (o.easicare.demo) S 152 152 0 0 -1 4202816 12650 0 0 0 70 68 0 0 ...
		 * 后面的四位分别代表的是
		 * 		utime 该任务在用户运行状态的时间
		 * 		stime 该任务在核心运行的时间
		 * 		cutime 所有已死线程在用户状态运行状态的时间
		 * 		cstime 所有已死线程在核心的运行时间
		 * 所以当前进程所占CPU的算法是:100*(processCpuTimeS-processCpuTimeF)/(totalCpuTimeS-totalCpuTimeF)
		 *
		 */
		String processPid = Integer.toString(pid);
		String cpuStatPath = "/proc/" + processPid + "/stat";
		try {
			// monitor cpu stat of certain process
			RandomAccessFile processCpuInfo = new RandomAccessFile(cpuStatPath, "r");
			String line = "";
			StringBuffer stringBuffer = new StringBuffer();
			stringBuffer.setLength(0);
			while ((line = processCpuInfo.readLine()) != null) {
				stringBuffer.append(line + "\n");
			}
			String[] tok = stringBuffer.toString().split(" ");
			processCpu = Long.parseLong(tok[13]) + Long.parseLong(tok[14]);
			processCpuInfo.close();
		} catch (FileNotFoundException e) {
			Log.w(LOG_TAG, "FileNotFoundException: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
		readTotalCpuStat();
	}

	/**
	 * read stat of each CPU cores
	 */
	private void readTotalCpuStat() {
		try {
			/* TODO 总的cpu的使用率：
			 * 在proc/stat下有详细的CPU使用情况,由于考虑到有多核的情况所以进行了while循环读取,但是实际看到后面发现,使用的数据只是使用了第一个总的cpu的时间就可以了.
			 * 文件的详细格式如下
			 * CPU 152342 1421 28562 1600830 12389 553 273 0 0
			 * CPU后面的几位数字分别是 :
			 * 		user 从系统启动开始累计到当前时刻，处于用户态的运行时间，不包含 nice值为负进程。
			 * 		nice 从系统启动开始累计到当前时刻，nice值为负的进程所占用的CPU时间;
			 * 		system 从系统启动开始累计到当前时刻，处于核心态的运行时间
			 * 		idle 从系统启动开始累计到当前时刻，除IO等待时间以外的其它等待时间
			 *		iowait 从系统启动开始累计到当前时刻，IO等待时间
			 *		irq 从系统启动开始累计到当前时刻，硬中断时间
			 *		softirq 从系统启动开始累计到当前时刻，软中断时间
			 *
			 * 所以CPU总数用率的算法是:100*((totalCpuTimeS-totalCpuTimeF) -(idelS-idelF))/ (totalCpuTimeS-totalCpuTimeF)
			 */
			// monitor total and idle cpu stat of certain process
			RandomAccessFile cpuInfo = new RandomAccessFile(CPU_STAT, "r");
			String line = "";
			while ((null != (line = cpuInfo.readLine())) && line.startsWith("cpu")) {
				String[] toks = line.split("\\s+");
				idleCpu.add(Long.parseLong(toks[4]));
				totalCpu.add(Long.parseLong(toks[1]) + Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
						+ Long.parseLong(toks[6]) + Long.parseLong(toks[5]) + Long.parseLong(toks[7]));
			}
			cpuInfo.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * get CPU name.
	 * 
	 * @return CPU name
	 */
	public String getCpuName() {
		try {
			RandomAccessFile cpuStat = new RandomAccessFile(CPU_INFO_PATH, "r");
			// check cpu type
			String line;
			while (null != (line = cpuStat.readLine())) {
				String[] values = line.split(":");
				if (values[0].contains(INTEL_CPU_NAME) || values[0].contains("Processor")) {
					cpuStat.close();
					Log.d(LOG_TAG, "CPU name="+values[1]);
					return values[1];
				}
			}
		} catch (IOException e) {
			Log.e(LOG_TAG, "IOException: " + e.getMessage());
		}
		return "";
	}

	/**
	 * display directories naming with "cpu*"
	 * 
	 * @author andrewleo
	 */
	class CpuFilter implements FileFilter {
		@Override
		public boolean accept(File pathname) {
			// Check if filename matchs "cpu[0-9]"
			if (Pattern.matches("cpu[0-9]", pathname.getName())) {
				return true;
			}
			return false;
		}
	}

	/**
	 * get CPU core numbers
	 * 
	 * @return cpu core numbers
	 */
	public int getCpuNum() {
		try {
			// Get directory containing CPU info
			File dir = new File(CPU_DIR_PATH);
			// Filter to only list the devices we care about
			File[] files = dir.listFiles(new CpuFilter());
			return files.length;
		} catch (Exception e) {
			e.printStackTrace();
			return 1;
		}
	}

	/**
	 * get CPU core list
	 * 
	 * @return cpu core list
	 */
	public ArrayList<String> getCpuList() {
		ArrayList<String> cpuList = new ArrayList<String>();
		try {
			// Get directory containing CPU info
			File dir = new File(CPU_DIR_PATH);
			// Filter to only list the devices we care about
			File[] files = dir.listFiles(new CpuFilter());
			for (int i = 0; i < files.length; i++) {
				cpuList.add(files[i].getName());
			}
			return cpuList;
		} catch (Exception e) {
			e.printStackTrace();
			cpuList.add("cpu0");
			return cpuList;
		}
	}

	/**
	 * 保留进程CPU和总CPU的使用率，同时收集网络流量。
	 * reserve used ratio of process CPU and total CPU, meanwhile collect
	 * network traffic.
	 *
	 * 网络流量，在一定的时间间隔内使用的进程CPU和总CPU的比例
	 * @return network traffic ,used ratio of process CPU and total CPU in
	 *         certain interval
	 */
	public ArrayList<String> getCpuRatioInfo(String totalBatt, String currentBatt, String temperature, String voltage, String fps, boolean isRoot) {

		String heapData = "";
		DecimalFormat fomart = new DecimalFormat();
		fomart.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		fomart.setGroupingUsed(false);
		fomart.setMaximumFractionDigits(2);
		fomart.setMinimumFractionDigits(2);

		cpuUsedRatio.clear();
		idleCpu.clear();
		totalCpu.clear();
		totalCpuRatio.clear();
		readCpuStat();

		try {
			String mDateTime2;
			Calendar cal = Calendar.getInstance();
			if ((Build.MODEL.equals("sdk")) || (Build.MODEL.equals("google_sdk"))) {
				mDateTime2 = formatterFile.format(cal.getTime().getTime() + 8 * 60 * 60 * 1000);
				totalBatt = Constants.NA;
				currentBatt = Constants.NA;
				temperature = Constants.NA;
				voltage = Constants.NA;
			} else
				mDateTime2 = formatterFile.format(cal.getTime().getTime());
			if (isInitialStatics) {
				preTraffic = trafficInfo.getTrafficInfo();
				isInitialStatics = false;
			} else {
				lastestTraffic = trafficInfo.getTrafficInfo();
				if (preTraffic == -1)
					traffic = -1;
				else {
					if (lastestTraffic > preTraffic) {
						traffic += (lastestTraffic - preTraffic + 1023) / 1024;
					}
				}
				preTraffic = lastestTraffic;
				Log.d(LOG_TAG, "lastestTraffic===" + lastestTraffic);
				Log.d(LOG_TAG, "preTraffic===" + preTraffic);
				StringBuffer totalCpuBuffer = new StringBuffer();
				//　这里首先判断上一次获取的数据是否有值,如果没有可能是第一次获取所以调到else中给totalCpu2复制
				if (null != totalCpu2 && totalCpu2.size() > 0) {
					// (应用占用的cpu时间-上次应用占用cpu时间)/(设备总的cpu时间-上次设备总的cpu时间)
					processCpuRatio = fomart.format(100 * ((double) (processCpu - processCpu2) / ((double) (totalCpu.get(0) - totalCpu2.get(0)))));
					//　以下是计算cpu的总的使用率
					for (int i = 0; i < (totalCpu.size() > totalCpu2.size() ? totalCpu2.size() : totalCpu.size()); i++) {
						String cpuRatio = "0.00";
						if (totalCpu.get(i) - totalCpu2.get(i) > 0) {
							cpuRatio = fomart
									.format(100 * ((double) ((totalCpu.get(i) - idleCpu.get(i)) - (totalCpu2.get(i) - idleCpu2.get(i))) / (double) (totalCpu
											.get(i) - totalCpu2.get(i))));
						}
						totalCpuRatio.add(cpuRatio);
						totalCpuBuffer.append(cpuRatio + Constants.COMMA);
					}
				} else {
					processCpuRatio = "0";
					totalCpuRatio.add("0");
					totalCpuBuffer.append("0,");
					totalCpu2 = (ArrayList<Long>) totalCpu.clone();
					processCpu2 = processCpu;
					idleCpu2 = (ArrayList<Long>) idleCpu.clone();
				}
				// 多核cpu的值写入csv文件中
				for (int i = 0; i < getCpuNum() - totalCpuRatio.size() + 1; i++) {
					totalCpuBuffer.append("0.00,");
				}
				long pidMemory = mi.getPidMemorySize(pid, context);
				String pMemory = fomart.format((double) pidMemory / 1024);
				long freeMemory = mi.getFreeMemorySize(context);
				String fMemory = fomart.format((double) freeMemory / 1024);
				String percent = context.getString(R.string.stat_error);
				if (totalMemorySize != 0) {
					percent = fomart.format(((double) pidMemory / (double) totalMemorySize) * 100);
				}

				if (isPositive(processCpuRatio) && isPositive(totalCpuRatio.get(0))) {
					String trafValue;
					// whether certain device supports traffic statics or not
					if (traffic == -1) {
						trafValue = Constants.NA;
					} else {
						trafValue = String.valueOf(traffic);
					}
					if(isRoot){
						String[][] heapArray = MemoryInfo.getHeapSize(pid, context);
						heapData = heapArray[0][1]+"/"+heapArray[0][0]+Constants.COMMA+heapArray[1][1]+"/"+heapArray[1][0]+Constants.COMMA;
					}
					EmmageeService.bw.write(mDateTime2 + Constants.COMMA + ProcessInfo.getTopActivity(context) + Constants.COMMA +heapData+ pMemory
							+ Constants.COMMA + percent + Constants.COMMA + fMemory + Constants.COMMA + processCpuRatio + Constants.COMMA
							+ totalCpuBuffer.toString() + trafValue + Constants.COMMA + totalBatt + Constants.COMMA + currentBatt + Constants.COMMA
							+ temperature + Constants.COMMA + voltage + Constants.COMMA + fps + Constants.LINE_END);
					totalCpu2 = (ArrayList<Long>) totalCpu.clone();
					processCpu2 = processCpu;
					idleCpu2 = (ArrayList<Long>) idleCpu.clone();
					cpuUsedRatio.add(processCpuRatio);
					cpuUsedRatio.add(totalCpuRatio.get(0));
					cpuUsedRatio.add(String.valueOf(traffic));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cpuUsedRatio;
	}


	public ArrayList<String> getCpuRatioInfo(boolean isRoot) {

		String heapData = "";
		DecimalFormat fomart = new DecimalFormat();
		fomart.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		fomart.setGroupingUsed(false);
		fomart.setMaximumFractionDigits(2);
		fomart.setMinimumFractionDigits(2);

		cpuUsedRatio.clear();
		idleCpu.clear();
		totalCpu.clear();
		totalCpuRatio.clear();
		readCpuStat();

		try {
			String mDateTime2;
			Calendar cal = Calendar.getInstance();
			if ((Build.MODEL.equals("sdk")) || (Build.MODEL.equals("google_sdk"))) {
				mDateTime2 = formatterFile.format(cal.getTime().getTime() + 8 * 60 * 60 * 1000);
			} else
				mDateTime2 = formatterFile.format(cal.getTime().getTime());
			if (isInitialStatics) {
				preTraffic = trafficInfo.getTrafficInfo();
				isInitialStatics = false;
			} else {
				lastestTraffic = trafficInfo.getTrafficInfo();
				if (preTraffic == -1)
					traffic = -1;
				else {
					if (lastestTraffic > preTraffic) {
						traffic += (lastestTraffic - preTraffic + 1023) / 1024;
					}
				}
				preTraffic = lastestTraffic;
				Log.d(LOG_TAG, "lastestTraffic===" + lastestTraffic);
				Log.d(LOG_TAG, "preTraffic===" + preTraffic);
				StringBuffer totalCpuBuffer = new StringBuffer();
				//　这里首先判断上一次获取的数据是否有值,如果没有可能是第一次获取所以调到else中给totalCpu2复制
				if (null != totalCpu2 && totalCpu2.size() > 0) {
					// (应用占用的cpu时间-上次应用占用cpu时间)/(设备总的cpu时间-上次设备总的cpu时间)
					processCpuRatio = fomart.format(100 * ((double) (processCpu - processCpu2) / ((double) (totalCpu.get(0) - totalCpu2.get(0)))));
					//　以下是计算cpu的总的使用率
					for (int i = 0; i < (totalCpu.size() > totalCpu2.size() ? totalCpu2.size() : totalCpu.size()); i++) {
						String cpuRatio = "0.00";
						if (totalCpu.get(i) - totalCpu2.get(i) > 0) {
							cpuRatio = fomart
									.format(100 * ((double) ((totalCpu.get(i) - idleCpu.get(i)) - (totalCpu2.get(i) - idleCpu2.get(i))) / (double) (totalCpu
											.get(i) - totalCpu2.get(i))));
						}
						totalCpuRatio.add(cpuRatio);
						totalCpuBuffer.append(cpuRatio + Constants.COMMA);
					}
				} else {
					processCpuRatio = "0";
					totalCpuRatio.add("0");
					totalCpuBuffer.append("0,");
					totalCpu2 = (ArrayList<Long>) totalCpu.clone();
					processCpu2 = processCpu;
					idleCpu2 = (ArrayList<Long>) idleCpu.clone();
				}
				// 多核cpu的值写入csv文件中
				for (int i = 0; i < getCpuNum() - totalCpuRatio.size() + 1; i++) {
					totalCpuBuffer.append("0.00,");
				}
				long pidMemory = mi.getPidMemorySize(pid, context);
				String pMemory = fomart.format((double) pidMemory / 1024);
				long freeMemory = mi.getFreeMemorySize(context);
				String fMemory = fomart.format((double) freeMemory / 1024);
				String percent = context.getString(R.string.stat_error);
				if (totalMemorySize != 0) {
					percent = fomart.format(((double) pidMemory / (double) totalMemorySize) * 100);
				}

				if (isPositive(processCpuRatio) && isPositive(totalCpuRatio.get(0))) {
					String trafValue;
					// whether certain device supports traffic statics or not
					if (traffic == -1) {
						trafValue = Constants.NA;
					} else {
						trafValue = String.valueOf(traffic);
					}
					if(isRoot){
						String[][] heapArray = MemoryInfo.getHeapSize(pid, context);
						heapData = heapArray[0][1]+"/"+heapArray[0][0]+Constants.COMMA+heapArray[1][1]+"/"+heapArray[1][0]+Constants.COMMA;
					}
					totalCpu2 = (ArrayList<Long>) totalCpu.clone();
					processCpu2 = processCpu;
					idleCpu2 = (ArrayList<Long>) idleCpu.clone();
					cpuUsedRatio.add(processCpuRatio);
					cpuUsedRatio.add(totalCpuRatio.get(0));
					cpuUsedRatio.add(String.valueOf(traffic));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cpuUsedRatio;
	}

	/**
	 * is text a positive number
	 * 
	 * @param text
	 * @return
	 */
	private boolean isPositive(String text) {
		Double num;
		try {
			num = Double.parseDouble(text);
		} catch (NumberFormatException e) {
			return false;
		}
		return num >= 0;
	}

}
