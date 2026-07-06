package com.skald.app.analysis;

import com.skald.app.model.ChatMessage;

import java.util.List;

/**
 * 对话构建器：将 ChatMessage 列表格式化为结构化文本
 *
 * 输出格式示例：
 * 对方: 你好啊
 * 我: 你好
 * 对方: 周末有空吗
 */
public class ConversationBuilder {

    /** 最大消息数（避免 token 过长） */
    private static final int MAX_MESSAGES = 20;

    /** 每条消息最大字符数 */
    private static final int MAX_CHARS_PER_MESSAGE = 200;

    /**
     * 构建对话文本
     */
    public String build(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int count = 0;

        // 取最近 N 条消息（截断旧消息）
        int startIndex = Math.max(0, messages.size() - MAX_MESSAGES);
        if (startIndex > 0) {
            sb.append("（...省略更早的消息...）\n\n");
        }

        for (int i = startIndex; i < messages.size() && count < MAX_MESSAGES; i++) {
            ChatMessage msg = messages.get(i);
            String text = truncateText(msg.text, MAX_CHARS_PER_MESSAGE);
            sb.append(msg.toString()).append("\n");
            count++;
        }

        return sb.toString();
    }

    /**
     * 截断过长文本
     */
    private String truncateText(String text, int maxChars) {
        if (text == null) return "";
        String trimmed = text.trim();
        if (trimmed.length() <= maxChars) {
            return trimmed;
        }
        return trimmed.substring(0, maxChars) + "...";
    }

    /**
     * 获取消息统计信息
     */
    public static String getStats(List<ChatMessage> messages) {
        int otherCount = 0;
        int selfCount = 0;
        for (ChatMessage msg : messages) {
            if (msg.speaker == ChatMessage.Speaker.OTHER) {
                otherCount++;
            } else {
                selfCount++;
            }
        }
        return "对方 " + otherCount + " 条，我 " + selfCount + " 条";
    }
}
