package com.skald.app.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PaddleOCR Lite 引擎实现（待集成）。
 *
 * 集成步骤：
 * 1. 下载 PP-OCRv4 模型和字典文件
 * 2. 放置到 app/src/main/assets/models/
 * 3. 添加 Paddle Lite 依赖
 * 4. 实现模型加载、推理、后处理逻辑
 *
 * 当前为占位实现，生产环境使用 MlKitOcrEngine。
 */
public class PaddleOcrEngine implements OcrEngine {

    private static final String TAG = "PaddleOcr";

    private final Context context;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private boolean ready = false;

    public PaddleOcrEngine(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newFixedThreadPool(2);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void initialize(InitCallback callback) {
        // TODO: 加载 Paddle Lite 模型
        Log.w(TAG, "PaddleOCR not yet integrated — use MlKitOcrEngine instead");
        mainHandler.post(() -> callback.onError("PaddleOCR 模型未集成"));
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void recognize(Bitmap bitmap, Callback callback) {
        mainHandler.post(() ->
                callback.onFailure(-1, "PaddleOCR 引擎未就绪"));
    }

    @Override
    public void release() {
        executor.shutdownNow();
        ready = false;
    }
}
