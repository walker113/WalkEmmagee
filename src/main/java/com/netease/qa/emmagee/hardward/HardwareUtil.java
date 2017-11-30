package com.netease.qa.emmagee.hardward;


import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;

import com.socks.library.KLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 硬件检测类
 */
public class HardwareUtil {


    public static boolean checkCamera(Context context) {

        PackageManager pm = context.getPackageManager();
        int numberCameras = Camera.getNumberOfCameras();
        KLog.e("camera : " +pm.hasSystemFeature(PackageManager.FEATURE_CAMERA));
        if (! (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)
                && pm.checkPermission("android.permission.CAMERA", context.getPackageName()) == PackageManager.PERMISSION_GRANTED
                && numberCameras > 0)) {
            return false;
        }

        Camera mCamera = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        try {
            for (int i = 0; i < numberCameras; i++) {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK
                        || cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    mCamera = Camera.open(i);
                    Camera.Parameters parameters = mCamera.getParameters();
                    mCamera.setParameters(parameters);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (mCamera != null)
                mCamera.release();
        }
        return false;
    }



    private static final int[] AUDIO_SOURCES = new int[] {
            MediaRecorder.AudioSource.DEFAULT,
            // 设置麦克风为音频源
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.CAMCORDER,
    };
    public static boolean checkVoice (Context context) {
        PackageManager pm = context.getPackageManager();
        if (pm.checkPermission("android.permission.RECORD_AUDIO", context.getPackageName())
                != PackageManager.PERMISSION_GRANTED)
            return false;


        AudioRecord audioRecord = null;
        int miniBuffer  = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        for (final int src: AUDIO_SOURCES) {
            try {

                audioRecord = new AudioRecord(src,
                        44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, miniBuffer);

                if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (audioRecord != null) {
                    audioRecord.release();
                    audioRecord = null;
                }
            }
        }

        return false;
    }


    public static boolean checkGps (Context context) {

        PackageManager pm = context.getPackageManager();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

        }

        if (!(pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS) &&
                pm.checkPermission("android.permission.ACCESS_FINE_LOCATION", context.getPackageName()) ==
                        PackageManager.PERMISSION_GRANTED)) {
            return false;
        }

        KLog.e(pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS));
        KLog.e(pm.checkPermission("android.permission.ACCESS_FINE_LOCATION", context.getPackageName()) ==
                PackageManager.PERMISSION_GRANTED);

        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

            LocationListener locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {

                }

                public void onStatusChanged(String provider, int status, Bundle extras) {}

                public void onProviderEnabled(String provider) {}

                public void onProviderDisabled(String provider) {}
            };

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            locationManager.removeUpdates(locationListener);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean checkNetwork(Context context) {
        NetworkInfo.State wifiState = NetworkInfo.State.UNKNOWN;
        NetworkInfo.State mobileState = NetworkInfo.State.UNKNOWN;
        NetworkInfo.State ethernetState = NetworkInfo.State.UNKNOWN;

        ConnectivityManager connectMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE) != null)
            mobileState = connectMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();

        if (connectMgr.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET) != null)
            ethernetState = connectMgr.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET).getState();

        if (connectMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI) != null)
            wifiState = connectMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();

        boolean isConnected = wifiState.equals(NetworkInfo.State.CONNECTED)
                || mobileState.equals(NetworkInfo.State.CONNECTED)
                || ethernetState.equals(NetworkInfo.State.CONNECTED);

        KLog.e("wifi     : " + wifiState.equals(NetworkInfo.State.CONNECTED));
        KLog.e("mobile   : " + mobileState.equals(NetworkInfo.State.CONNECTED));
        KLog.e("ethernet : " + ethernetState.equals(NetworkInfo.State.CONNECTED));

        KLog.e(Ping("127.0.0.1"));
        return isConnected;
    }


    public static boolean Ping(String str) {
        Process process;
        try {
            //ping -c 3 -w 100  中  ，-c 是指ping的次数 3是指ping 3次 ，-w 100  以秒为单位指定超时间隔，是指超时时间为100秒
            process = Runtime.getRuntime().exec("ping -c 3 -w 100 " + str);
            int status = process.waitFor();

            InputStream input = process.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            StringBuilder buffer = new StringBuilder();
            String line = "";
            while ((line = in.readLine()) != null){
                buffer.append(line).append("\n");
            }
            KLog.e("Return ============\n" + buffer.toString());

            if (status == 0)
                return true;

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

}
