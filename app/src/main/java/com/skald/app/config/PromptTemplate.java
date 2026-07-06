package com.skald.app.config;

/**
 * 提示词模板管理
 */
public class PromptTemplate {

    /** 默认系统提示词 */
    public static final String DEFAULT_SYSTEM_PROMPT =
        "你是一个聊天回复助手。根据用户提供的聊天上下文，生成3条回复建议。\n" +
        "每条回复需要标注风格类型：\n" +
        "1. 【保守】- 安全、礼貌、不容易出错的回复\n" +
        "2. 【激进】- 积极推进关系或话题的回复\n" +
        "3. 【出其不意】- 有趣、幽默或创意的回复\n\n" +
        "请严格按以下格式输出（每条之间用三个减号分隔）：\n" +
        "【保守】<回复内容>\n" +
        "---\n" +
        "【激进】<回复内容>\n" +
        "---\n" +
        "【出其不意】<回复内容>";

    /** 获取有效的系统提示词（用户自定义 > 默认） */
    public static String getEffectivePrompt(AppConfig config) {
        String custom = config.getCustomPrompt();
        if (custom != null && !custom.trim().isEmpty()) {
            return custom;
        }
        return DEFAULT_SYSTEM_PROMPT;
    }

    /**
     * 构建完整的 API 请求 messages 数组
     */
    public static String buildUserMessage(String conversation) {
        return "以下是聊天对话内容，请根据上下文生成回复建议：\n\n" + conversation;
    }
}
