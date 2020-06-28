package com.example.stepsapp.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.stepsapp.data.DataBaseManager;
import com.example.stepsapp.util.API23Wrapper;
import com.example.stepsapp.util.DateUtil;

import java.util.Date;

import static com.example.stepsapp.test.App.CHANNEL_ID;

public class SensorService extends Service implements SensorEventListener{
    public final static int NOTIFICATION_ID = 1;
    private final static long MICROSECONDS_IN_ONE_MINUTE = 60000000;
    private final static long SAVE_OFFSET_TIME = AlarmManager.INTERVAL_HOUR;
    private final static int SAVE_OFFSET_STEPS = 500;

    private static int steps;
    private static int lastSaveSteps;
    private static long lastSaveTime;

    //7
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("STEP", "onCreate SesorService");
    }

    //5
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //6
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("STEP", "onStartCommand");
        reRegisterSensor();
        long nextUpdate = Math.min(DateUtil.getTomorrow(),
                System.currentTimeMillis() + AlarmManager.INTERVAL_HOUR);

        AlarmManager am =
                (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent
                .getService(getApplicationContext(), 2, new Intent(this, SensorService.class),
                        PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= 23) {
            API23Wrapper.setAlarmWhileIdle(am, AlarmManager.RTC, nextUpdate, pi);
        } else {
            am.set(AlarmManager.RTC, nextUpdate, pi);
        }
        return START_STICKY;
    }

    //12
    private void reRegisterSensor() {
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        try {
            sensorManager.unregisterListener(this);
        } catch (Exception e) {
            Log.d("STEP", "Catch unregisterListener");
        }

        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        try {
            sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_NORMAL, (int) (5 * MICROSECONDS_IN_ONE_MINUTE));
        } catch (Exception e) {
            Log.d("STEP", "Catch registerListener");
        }
    }

    //2
    @Override
    public void onSensorChanged(SensorEvent event) {
        steps = (int) event.values[0];
        updateDataBaseIfNecessary();
    }

    //3
    private boolean updateDataBaseIfNecessary() {
        if (steps > lastSaveSteps + SAVE_OFFSET_STEPS ||
                (steps > 0 && System.currentTimeMillis() > lastSaveTime + SAVE_OFFSET_TIME)) {
            DataBaseManager db = DataBaseManager.getInstance(this);

            // ngày hôm nay chưa có trong database
            if (db.getSteps(DateUtil.getToday()) == Integer.MIN_VALUE) {
                int pauseDifference = steps -
                        getSharedPreferences("stepsapp", Context.MODE_PRIVATE)
                                .getInt("pauseCount", steps);
                db.insertNewDay(DateUtil.getToday(), steps - pauseDifference);
                if (pauseDifference > 0) {

                    //update
                    getSharedPreferences("stepsapp", Context.MODE_PRIVATE).edit().putInt("pauseCount", steps).apply();
                }
            }
            db.saveCurrentSteps(steps);
            db.close();
            lastSaveSteps = steps;
            lastSaveTime = System.currentTimeMillis();

            showNotification(); // update notification
            //WidgetUpdateService.enqueueUpdate(this);
            return true;
        }

        return false;
    }

    //4
    private void showNotification() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Content Title").build();

        startForeground(1, notification);
    }

    //1
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d("STEP", "onAccuracyChanged");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        ((AlarmManager) getSystemService(Context.ALARM_SERVICE))
                .set(AlarmManager.RTC, System.currentTimeMillis() + 500, PendingIntent
                        .getService(this, 3, new Intent(this, SensorService.class), 0));
    }
}
