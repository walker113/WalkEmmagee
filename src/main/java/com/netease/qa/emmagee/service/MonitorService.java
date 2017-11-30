package com.netease.qa.emmagee.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.text.method.MetaKeyKeyListener;

import com.netease.qa.emmagee.R;
import com.netease.qa.emmagee.activity.MainPageActivity;
import com.netease.qa.emmagee.hardward.HardwareUtil;
import com.netease.qa.emmagee.utils.Constants;
import com.netease.qa.emmagee.utils.CpuInfo;
import com.netease.qa.emmagee.utils.MemoryInfo;
import com.netease.qa.emmagee.utils.ProcessInfo;
import com.socks.library.KLog;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;

public class MonitorService extends Service {

    private int pid, uid;
    private String processName, packageName, startActivity;
    private CpuInfo cpuInfo;
    private ProcessInfo procInfo;
    private MemoryInfo memoryInfo;
    private Handler handler = new Handler();
    private DecimalFormat format;

    @Override
    public void onCreate() {
        super.onCreate();
        KLog.e();
        memoryInfo = new MemoryInfo();
        procInfo = new ProcessInfo();

        format = new DecimalFormat();
        format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
        format.setGroupingUsed(false);
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(0);


        Intent intent = new Intent(this, MainPageActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("This is content title")
                .setContentText("This is content text")
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.icon)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon))
                .setContentIntent(pi)
                .build();

        startForeground(1, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        KLog.e();
        pid = intent.getExtras().getInt("pid");
        processName = intent.getExtras().getString("processName");
        packageName = intent.getExtras().getString("packageName");

        KLog.e("\npid = " + pid + "\nprocessName = " + processName + "\npackageName = " + packageName);

        try {
            PackageManager pm = getPackageManager();
//			ApplicationInfo ainfo = pm.getApplicationInfo(packageName, PackageManager.GET_ACTIVITIES);
            ApplicationInfo ainfo = pm.getApplicationInfo(packageName, 0);
            uid = ainfo.uid;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        KLog.e("uid = " + uid);
        cpuInfo = new CpuInfo(getBaseContext(), pid, Integer.toString(uid));


        handler.postDelayed(task, 1000);

        return super.onStartCommand(intent, flags, startId);
    }

    private static final String BLANK_STRING = "";
    private Runnable task = new Runnable() {
        public void run() {
            dataRefresh();
            handler.postDelayed(this, 5 * 1000);
        }
    };

    private void dataRefresh() {
        int pidMemory = memoryInfo.getPidMemorySize(pid, getBaseContext());
        long freeMemory = memoryInfo.getFreeMemorySize(getBaseContext());
        String freeMemoryKb = format.format((double) freeMemory / 1024);
        String processMemory = format.format((double) pidMemory / 1024);
        KLog.e(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        KLog.e("应用/剩余内存 : " + processMemory + "/" + freeMemoryKb + "MB");

        ArrayList<String> processInfo = cpuInfo.getCpuRatioInfo(false);

        String processCpuRatio = "0.00";
        String totalCpuRatio = "0.00";
        String trafficSize = "0";
        long tempTraffic = 0L;
        double trafficMb = 0;
        boolean isMb = false;

        if (!processInfo.isEmpty()) {
            processCpuRatio = processInfo.get(0);
            totalCpuRatio = processInfo.get(1);
            trafficSize = processInfo.get(2);
            if (!(BLANK_STRING.equals(trafficSize))
                    && !("-1".equals(trafficSize))) {
                tempTraffic = Long.parseLong(trafficSize);
                if (tempTraffic > 1024) {
                    isMb = true;
                    trafficMb = (double) tempTraffic / 1024;
                }
            }

            KLog.e("应用/总体CPU : " + processCpuRatio + "%/" + totalCpuRatio + "%");

            if ("-1".equals(trafficSize)) {
                KLog.e("流量(KB)" + Constants.COMMA
                        + getString(R.string.traffic) + Constants.NA);
            } else if (isMb)
                KLog.e("流量(KB)" + Constants.COMMA
                        + getString(R.string.traffic)
                        + format.format(trafficMb) + "MB");
            else
                KLog.e("流量(KB)" + Constants.COMMA
                        + getString(R.string.traffic) + trafficSize
                        + "KB");
        }

        KLog.e("相机 : " + HardwareUtil.checkCamera(getApplicationContext()));
        KLog.e("麦克风 : " + HardwareUtil.checkVoice(getApplicationContext()));
        KLog.e("定位 : " + HardwareUtil.checkGps(getApplicationContext()));
        KLog.e("网络 : " + HardwareUtil.checkNetwork(getApplicationContext()));
    }
}
