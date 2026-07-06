package com.skald.app;

import android.app.Application;
import android.util.Log;

import com.skald.app.config.AppConfig;

/**
 * Application 入口：全局初始化
 */
public class SkaldApplication extends Application {

    private static final String TAG = "SkaldApp";

    private static SkaldApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // 初始化配置
        AppConfig config = AppConfig.getInstance(this);

        // 进程重启后清除服务状态标志（进程被杀时服务也停止了）
        config.setServiceRunning(false);

        Log.i(TAG, "Skald Application initialized, API configured: " + config.isApiConfigured());
    }

    public static SkaldApplication getInstance() {
        return instance;
    }
}
