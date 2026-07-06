package com.skald.app.ocr;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.skald.app.model.OcrResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 演示用 OCR 引擎：返回模拟的聊天数据用于测试完整流水线。
 *
 * 生产环境请替换为 PaddleOcrEngine（需集成 PaddleOCR Lite 模型文件）。
 */
public class DemoOcrEngine implements OcrEngine {

    private static final String TAG = "DemoOcr";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // 模拟微信聊天界面的 OCR 结果（左侧=对方，右侧=我）
    private static final String[][] SAMPLE_CONVERSATION = {
        // {text, left, top, right, bottom}
        {"在吗？",        "60", "200", "350", "250"},
        {"在的，什么事？",  "700", "270", "1050", "320"},
        {"周末有空吗，一起吃饭？", "60", "350", "500", "400"},
        {"好啊，去哪里？",  "700", "420", "950", "470"},
        {"去上次那家川菜馆怎么样？", "60", "500", "550", "550"},
        {"可以，几点？",   "700", "570", "880", "620"},
        {"晚上7点吧",     "60", "650", "330", "700"},
        {"好的，到时候见",  "700", "720", "950", "770"},
        {"嗯嗯，记得带伞，明天可能下雨", "60", "800", "600", "850"},
        {"谢谢提醒，你也是", "700", "870", "1020", "920"},
    };

    private boolean ready = true;

    @Override
    public void initialize(InitCallback callback) {
        ready = true;
        mainHandler.post(callback::onReady);
        Log.i(TAG, "Demo OCR engine ready");
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void recognize(Bitmap bitmap, Callback callback) {
        Log.i(TAG, "Demo OCR: returning sample conversation data");

        List<OcrResult> results = new ArrayList<>();
        for (String[] item : SAMPLE_CONVERSATION) {
            String text = item[0];
            int left = Integer.parseInt(item[1]);
            int top = Integer.parseInt(item[2]);
            int right = Integer.parseInt(item[3]);
            int bottom = Integer.parseInt(item[4]);

            results.add(new OcrResult(text, new Rect(left, top, right, bottom), 0.95f));
        }

        // 模拟一点延迟，与真实 OCR 体验一致
        mainHandler.postDelayed(() -> callback.onSuccess(results), 500);
    }

    @Override
    public void release() {
        ready = false;
    }
}
