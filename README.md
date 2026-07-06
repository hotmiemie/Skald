# Skald — 智能聊天话术助手

基于 Android 无障碍服务的聊天辅助工具。一键截图 → OCR 识别 → DeepSeek AI 生成多风格回复建议(可替换模型)。

## 声明
项目基于10-neon老师的ratatoskr项目设计，项目地址 https://github.com/10-neon/ratatoskr
原项目基于node信息获取对话，因为微信更新导致获取节点信息困难，本项目采取ocr方式
如有侵权请联系我删除项目，谢谢，抱歉

## 功能

- 📸 **一键截图分析**：通过悬浮按钮，在微信/QQ 等聊天界面一键截图
- 🔍 **端侧 OCR**：基于 PaddleOCR Lite，完全离线识别屏幕文字
- 🧠 **AI 话术建议**：调用 DeepSeek API，生成 3 种风格的回复建议
- 🎨 **风格区分**：
  - 【保守】安全、礼貌、不易出错
  - 【激进】积极推进关系或话题
  - 【出其不意】有趣、幽默、创意
- 📋 **一键复制**：点击任意话术自动复制到剪贴板
- 🔒 **隐私优先**：截图仅在内存中处理，不保存到相册，不上传至第三方

## 系统要求

- Android 9.0 (API 28) 及以上
- 需要开启：无障碍服务、悬浮窗权限、通知权限

## 快速开始

### 1. 安装与配置

1. 安装 APK 到 Android 设备
2. 打开 Skald，进入「权限向导」依次授权：
   - **无障碍服务**：跳转系统设置 → 找到 Skald → 开启
   - **悬浮窗权限**：允许在其他应用上层显示
   - **通知权限**：允许发送通知（保持后台运行）

### 2. 配置 DeepSeek API

在 Skald 主界面填写：
- **API 地址**：如`https://api.deepseek.com`
- **模型名称**：如`deepseek-chat`（或其他可用模型）
- **API Key**：如在 [DeepSeek 平台](https://platform.deepseek.com) 获取

点击「测试连接」验证配置是否正确。

### 3. 使用

1. 点击「开始服务」启动悬浮按钮
2. 进入微信 聊天界面
3. 点击悬浮按钮 💬
4. 等待分析完成（约 2-5 秒）
5. 在弹出的结果窗中选择合适的回复话术
6. 话术自动复制到剪贴板，直接粘贴即可

### 4. 自定义提示词（可选）

在「自定义提示词」区域编辑系统提示词，可以调整 AI 的回复风格或增加特殊要求。留空则使用默认提示词。

## 技术架构

```
悬浮按钮点击
    ↓
AccessibilityService.takeScreenshot()  ← 无障碍服务截图
    ↓
PaddleOCR Lite (端侧离线识别)           ← OCR 文字提取
    ↓
TextPositionAnalyzer                   ← 根据文字位置判断说话人
    ↓
ConversationBuilder                    ← 构建格式化对话文本
    ↓
DeepSeek API (/v1/chat/completions)    ← HTTP 请求生成话术
    ↓
SuggestionParser                       ← 解析 3 条风格建议
    ↓
FloatResultWindow                      ← 悬浮窗展示 + 点击复制
```

### 技术栈

| 组件 | 技术选型 |
|------|----------|
| 主语言 | Java 11 |
| 构建系统 | Gradle 8.7 + AGP 8.4 |
| minSdkVersion | 28 (Android 9.0) |
| OCR 引擎 | PaddleOCR Lite（离线） |
| HTTP 客户端 | OkHttp 4.12 |
| JSON 解析 | Gson |
| UI 框架 | AndroidX + Material Components |

## 项目结构

```
Skald/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/skald/app/
│       │   ├── SkaldApplication.java
│       │   ├── MainActivity.java                  # 配置主界面
│       │   ├── service/
│       │   │   └── SkaldAccessibilityService.java  # 无障碍服务核心
│       │   ├── floatwindow/
│       │   │   ├── FloatButtonManager.java         # 悬浮按钮
│       │   │   ├── FloatResultWindow.java          # 结果悬浮窗
│       │   │   └── FloatWindowService.java         # 前台服务
│       │   ├── screenshot/
│       │   │   ├── ScreenshotProvider.java         # 截图接口
│       │   │   └── AccessibilityScreenshot.java    # API 28+ 实现
│       │   ├── ocr/
│       │   │   ├── OcrEngine.java                  # OCR 接口
│       │   │   └── PaddleOcrEngine.java            # PaddleOCR 实现
│       │   ├── analysis/
│       │   │   ├── TextPositionAnalyzer.java       # 位置判断
│       │   │   └── ConversationBuilder.java        # 对话构建
│       │   ├── api/
│       │   │   ├── DeepSeekClient.java             # API 客户端
│       │   │   └── SuggestionParser.java           # 响应解析
│       │   ├── config/
│       │   │   ├── AppConfig.java                  # 配置管理
│       │   │   └── PromptTemplate.java             # 提示词模板
│       │   ├── model/
│       │   │   ├── OcrResult.java
│       │   │   ├── ChatMessage.java
│       │   │   └── Suggestion.java
│       │   ├── clipboard/
│       │   │   └── ClipboardHelper.java
│       │   ├── permission/
│       │   │   ├── PermissionChecker.java
│       │   │   └── PermissionGuideActivity.java
│       │   └── util/
│       │       └── ScreenUtils.java
│       └── res/
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## 集成 PaddleOCR Lite（未来计划）

当前使用 Google ML Kit 中文识别引擎。如需更高识别率，可集成 PaddleOCR Lite：

1. 下载 PP-OCRv4 模型：从 [PaddleOCR 模型列表](https://github.com/PaddlePaddle/PaddleOCR/blob/main/doc/doc_ch/models_list.md) 获取
2. 放置到 `app/src/main/assets/models/`
3. 添加 Paddle Lite 依赖到 `build.gradle`
4. 启用 `PaddleOcrEngine` 替换 `MlKitOcrEngine`

详见 `PaddleOcrEngine.java` 中的注释。

## 构建

```bash
# Windows
gradlew.bat assembleDebug

# Linux / macOS
./gradlew assembleDebug
```

构建产物位于 `app/build/outputs/apk/debug/`。

### 环境配置

项目已配置国内镜像加速：
- **Gradle**：腾讯云镜像
- **Maven**：阿里云镜像

## 隐私说明

- 📷 截图**仅在内存中**处理（Bitmap），OCR 完成后立即释放
- 🚫 截图**不会**保存到手机相册或文件系统
- 🌐 仅将 OCR 提取的**文字内容**发送至您配置的 DeepSeek API
- 🔑 API Key 仅存储在本地 SharedPreferences
- 📊 **不会**收集任何用户数据或使用统计

## 许可证

MIT License

---

**Skald** 名字来源于古挪威语的"宫廷诗人"，寓意"语言的艺术"。
