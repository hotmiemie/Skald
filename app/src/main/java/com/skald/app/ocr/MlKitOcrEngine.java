package com.skald.app.ocr;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.skald.app.model.OcrResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 基于 Google ML Kit 的真实 OCR 引擎
 * 支持中文、端侧离线识别，首次使用会自动下载语言模型
 */
public class MlKitOcrEngine implements OcrEngine {

    private static final String TAG = "MlKitOcr";

    private TextRecognizer recognizer;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private boolean ready;

    public MlKitOcrEngine() {
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.ready = false;
    }

    @Override
    public void initialize(InitCallback callback) {
        try {
            recognizer = TextRecognition.getClient(
                    new ChineseTextRecognizerOptions.Builder().build());
            ready = true;
            Log.i(TAG, "ML Kit OCR engine ready");
            mainHandler.post(callback::onReady);
        } catch (Exception e) {
            Log.e(TAG, "Failed to init ML Kit OCR", e);
            mainHandler.post(() -> callback.onError(e.getMessage()));
        }
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void recognize(Bitmap bitmap, Callback callback) {
        if (!ready || recognizer == null) {
            mainHandler.post(() -> callback.onFailure(-1, "OCR 引擎未就绪"));
            return;
        }

        if (bitmap == null || bitmap.isRecycled()) {
            mainHandler.post(() -> callback.onFailure(-2, "无效的图片"));
            return;
        }

        executor.execute(() -> {
            try {
                // 增强对比度以提升中文字符识别准确率
                Bitmap enhanced = enhanceContrast(bitmap);

                // 将增强后的 Bitmap 转为 ML Kit InputImage
                InputImage inputImage = InputImage.fromBitmap(enhanced, 0);

                // 执行识别（同步等待结果再回调）
                recognizer.process(inputImage)
                        .addOnSuccessListener(executor, text -> {
                            List<OcrResult> results = extractResults(text);
                            Log.i(TAG, "ML Kit OCR: " + results.size() + " text blocks found");
                            enhanced.recycle();
                            mainHandler.post(() -> callback.onSuccess(results));
                        })
                        .addOnFailureListener(executor, e -> {
                            Log.e(TAG, "ML Kit OCR failed: " + e.getMessage());
                            enhanced.recycle();
                            mainHandler.post(() ->
                                    callback.onFailure(-3, "OCR 识别失败: " + e.getMessage()));
                        });
            } catch (Exception e) {
                Log.e(TAG, "OCR error", e);
                mainHandler.post(() -> callback.onFailure(-4, e.getMessage()));
            }
        });
    }

    /**
     * 增强 Bitmap 对比度，让文字更清晰，提升 OCR 准确率
     */
    private Bitmap enhanceContrast(Bitmap source) {
        // 对比度因子：1.0 = 不变，>1.0 = 增强
        float contrast = 1.4f;
        float translate = (1f - contrast) * 128f;

        ColorMatrix cm = new ColorMatrix(new float[]{
                contrast, 0, 0, 0, translate,
                0, contrast, 0, 0, translate,
                0, 0, contrast, 0, translate,
                0, 0, 0, 1, 0
        });

        Bitmap enhanced = Bitmap.createBitmap(source.getWidth(), source.getHeight(), source.getConfig());
        Canvas canvas = new Canvas(enhanced);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(source, 0, 0, paint);

        return enhanced;
    }

    @Override
    public void release() {
        executor.shutdownNow();
        if (recognizer != null) {
            try {
                recognizer.close();
            } catch (Exception e) {
                Log.e(TAG, "Failed to close recognizer", e);
            }
        }
        ready = false;
        Log.i(TAG, "ML Kit OCR engine released");
    }

    /**
     * 从 ML Kit Text 结果中提取 OcrResult 列表
     * 只提取行级别的文本（line），粒度正好适合微信聊天气泡
     */
    private List<OcrResult> extractResults(Text text) {
        List<OcrResult> results = new ArrayList<>();

        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                String lineText = line.getText();
                Rect boundingBox = line.getBoundingBox();

                if (lineText == null || lineText.trim().isEmpty()) continue;
                if (boundingBox == null) continue;

                // ML Kit 的 confidence 在 Line 级别可能不可用，设默认值
                float confidence = 0.85f;
                try {
                    // 尝试获取置信度
                    confidence = line.getConfidence();
                    if (confidence <= 0) confidence = 0.85f;
                } catch (Exception ignored) {
                }

                results.add(new OcrResult(lineText.trim(), boundingBox, confidence));
            }
        }

        return results;
    }
}
