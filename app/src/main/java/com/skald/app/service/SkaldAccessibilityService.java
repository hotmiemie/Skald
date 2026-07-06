package com.skald.app.service;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.skald.app.analysis.ConversationBuilder;
import com.skald.app.analysis.TextPositionAnalyzer;
import com.skald.app.api.DeepSeekClient;
import com.skald.app.api.SuggestionParser;
import com.skald.app.config.AppConfig;
import com.skald.app.config.PromptTemplate;
import com.skald.app.floatwindow.FloatResultWindow;
import com.skald.app.model.ChatMessage;
import com.skald.app.model.OcrResult;
import com.skald.app.model.Suggestion;
import com.skald.app.ocr.MlKitOcrEngine;
import com.skald.app.ocr.OcrEngine;
import com.skald.app.screenshot.AccessibilityScreenshot;
import com.skald.app.screenshot.ScreenshotProvider;
import com.skald.app.util.ScreenUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 无障碍服务核心：
 * - 监听前台应用变化
 * - 通过静态实例接收悬浮按钮触发
 * - 执行截图 → OCR → 分析 → API → 展示的完整流程
 */
public class SkaldAccessibilityService extends AccessibilityService {

    private static final String TAG = "SkaldA11ySvc";

    /** 静态实例，供 FloatWindowService 直接调用 */
    private static SkaldAccessibilityService instance;

    private Handler mainHandler;
    private ScreenshotProvider screenshotProvider;
    private OcrEngine ocrEngine;
    private DeepSeekClient deepSeekClient;
    private FloatResultWindow resultWindow;
    private AppConfig config;

    private boolean isProcessing = false;
    /** 当前处理的对话文本，用于结果展示 */
    private String currentConversation;

    /**
     * 获取静态实例（可能为 null）
     */
    public static SkaldAccessibilityService getInstance() {
        return instance;
    }

    /**
     * 由 FloatWindowService 调用，触发截图流程
     */
    public static boolean triggerFromService() {
        if (instance == null) {
            Log.w(TAG, "AccessibilityService instance is null, cannot trigger");
            return false;
        }
        instance.mainHandler.post(() -> instance.executeFullPipeline());
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        mainHandler = new Handler(Looper.getMainLooper());
        config = AppConfig.getInstance(this);

        // 初始化截图提供者
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            screenshotProvider = new AccessibilityScreenshot(this);
        }

        // 初始化结果悬浮窗
        resultWindow = new FloatResultWindow(this);

        // 初始化 DeepSeek 客户端
        deepSeekClient = new DeepSeekClient();

        // 初始化 OCR 引擎（ML Kit 中文识别，首次使用自动下载模型）
        ocrEngine = new MlKitOcrEngine();
        ocrEngine.initialize(new OcrEngine.InitCallback() {
            @Override
            public void onReady() {
                Log.i(TAG, "OCR engine ready (ML Kit)");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "OCR engine init failed: " + error);
            }
        });

        Log.i(TAG, "AccessibilityService created, instance ready");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = event.getPackageName() != null
                    ? event.getPackageName().toString() : "";
            Log.d(TAG, "Foreground app: " + packageName);
        }
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "AccessibilityService interrupted");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;

        if (screenshotProvider != null) {
            screenshotProvider.release();
        }
        if (ocrEngine != null) {
            ocrEngine.release();
        }
        if (resultWindow != null) {
            resultWindow.hide();
        }

        Log.i(TAG, "AccessibilityService destroyed");
    }

    /**
     * 设置 OCR 引擎（由外部注入，支持切换不同实现）
     */
    public void setOcrEngine(OcrEngine engine) {
        this.ocrEngine = engine;
    }

    // ==================== 流水线 ====================

    private void executeFullPipeline() {
        if (isProcessing) {
            Log.w(TAG, "Already processing, skip");
            return;
        }

        isProcessing = true;
        resultWindow.showLoading();
        Log.i(TAG, "Starting full pipeline...");
        doScreenshot();
    }

    private void doScreenshot() {
        if (screenshotProvider == null || !screenshotProvider.isAvailable()) {
            resultWindow.showError("当前设备不支持无障碍截图（需要 Android 9.0+）");
            isProcessing = false;
            return;
        }

        screenshotProvider.takeScreenshot(new ScreenshotProvider.Callback() {
            @Override
            public void onSuccess(Bitmap bitmap) {
                Log.i(TAG, "Screenshot captured, starting OCR...");
                doOcr(bitmap);
            }

            @Override
            public void onFailure(int errorCode, String errorMessage) {
                Log.e(TAG, "Screenshot failed: " + errorMessage);
                resultWindow.showError("截图失败: " + errorMessage);
                isProcessing = false;
            }
        });
    }

    private void doOcr(Bitmap bitmap) {
        if (ocrEngine == null || !ocrEngine.isReady()) {
            Log.w(TAG, "OCR engine not ready, returning empty");
            if (bitmap != null) bitmap.recycle();
            resultWindow.showError("OCR 引擎未就绪");
            isProcessing = false;
            return;
        }

        ocrEngine.recognize(bitmap, new OcrEngine.Callback() {
            @Override
            public void onSuccess(List<OcrResult> results) {
                Log.i(TAG, "OCR completed, " + results.size() + " blocks");
                handleOcrComplete(results, bitmap);
            }

            @Override
            public void onFailure(int errorCode, String errorMessage) {
                Log.e(TAG, "OCR failed: " + errorMessage);
                bitmap.recycle();
                resultWindow.showError("文字识别失败: " + errorMessage);
                isProcessing = false;
            }
        });
    }

    private void handleOcrComplete(List<OcrResult> ocrResults, Bitmap screenshot) {
        if (ocrResults.isEmpty()) {
            resultWindow.showError("未识别到任何文字，请尝试在聊天界面点击");
            screenshot.recycle();
            isProcessing = false;
            return;
        }

        int screenWidth = ScreenUtils.getScreenWidth(this);
        int statusBarH = ScreenUtils.getStatusBarHeight(this);
        TextPositionAnalyzer analyzer = new TextPositionAnalyzer(screenWidth, statusBarH, 56);
        // 传入截图供颜色检测
        analyzer.setScreenshot(screenshot);
        List<ChatMessage> messages = analyzer.analyze(ocrResults);

        // 颜色检测完成，回收截图
        screenshot.recycle();

        if (messages.isEmpty()) {
            resultWindow.showError("未能识别到有效的对话内容");
            isProcessing = false;
            return;
        }

        ConversationBuilder builder = new ConversationBuilder();
        currentConversation = builder.build(messages);
        Log.i(TAG, "Conversation built:\n" + currentConversation);

        doApiQuery(currentConversation);
    }

    private void doApiQuery(String conversation) {
        if (!config.isApiConfigured()) {
            resultWindow.showError("请先在主界面配置 DeepSeek API Key");
            isProcessing = false;
            return;
        }

        String systemPrompt = PromptTemplate.getEffectivePrompt(config);
        String userMessage = PromptTemplate.buildUserMessage(conversation);

        DeepSeekClient.QueryRequest request = new DeepSeekClient.QueryRequest(
                config.getApiUrl(),
                config.getApiKey(),
                config.getApiModel(),
                systemPrompt,
                userMessage
        );

        deepSeekClient.query(request, new DeepSeekClient.Callback() {
            @Override
            public void onSuccess(String rawResponse) {
                Log.i(TAG, "DeepSeek API response received");
                List<Suggestion> suggestions = SuggestionParser.parse(rawResponse);
                if (suggestions.isEmpty()) {
                    suggestions = new ArrayList<>();
                    suggestions.add(new Suggestion(rawResponse));
                }
                resultWindow.showSuggestions(currentConversation, suggestions);
                isProcessing = false;
            }

            @Override
            public void onFailure(int statusCode, String errorMessage) {
                Log.e(TAG, "DeepSeek API failed: " + statusCode + " " + errorMessage);
                resultWindow.showError("API 请求失败: " + errorMessage);
                isProcessing = false;
            }
        });
    }

}
