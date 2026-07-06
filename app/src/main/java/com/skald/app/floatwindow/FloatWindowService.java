package com.skald.app.floatwindow;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.skald.app.MainActivity;
import com.skald.app.R;
import com.skald.app.config.AppConfig;
import com.skald.app.service.SkaldAccessibilityService;

/**
 * 前台服务：维持悬浮按钮生命周期，防止被系统杀死
 */
public class FloatWindowService extends Service {

    private static final String TAG = "FloatWindowSvc";
    private static final String CHANNEL_ID = "skald_float_service";
    private static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_START = "com.skald.app.START_FLOAT";
    public static final String ACTION_STOP = "com.skald.app.STOP_FLOAT";

    private FloatButtonManager floatButtonManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service onCreate");
        AppConfig.getInstance(this).setServiceRunning(true);
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service onStartCommand");

        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                AppConfig.getInstance(this).setServiceRunning(true);
                showFloatButton();
            } else if (ACTION_STOP.equals(action)) {
                hideFloatButton();
                stopSelf();
            }
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AppConfig.getInstance(this).setServiceRunning(false);
        hideFloatButton();
        Log.i(TAG, "Service onDestroy");
    }

    private void showFloatButton() {
        if (floatButtonManager == null) {
            floatButtonManager = new FloatButtonManager(this);
            floatButtonManager.setOnFloatButtonClickListener(() -> {
                Log.i(TAG, "Float button clicked");
                boolean triggered = SkaldAccessibilityService.triggerFromService();
                if (!triggered) {
                    Toast.makeText(FloatWindowService.this,
                            "无障碍服务未运行，请先在系统设置中开启", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (!floatButtonManager.isShowing()) {
            floatButtonManager.show();
        }
    }

    private void hideFloatButton() {
        if (floatButtonManager != null) {
            floatButtonManager.hide();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Skald 辅助服务运行状态");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW);

        return builder.build();
    }
}
