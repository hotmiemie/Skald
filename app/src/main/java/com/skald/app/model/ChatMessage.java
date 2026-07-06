package com.skald.app.model;

/**
 * 一条对话消息：包含说话人标识和文字内容
 */
public class ChatMessage {

    /** 说话人标识 */
    public enum Speaker {
        /** 对方（消息在屏幕左侧） */
        OTHER("对方"),
        /** 自己（消息在屏幕右侧） */
        SELF("我");

        private final String label;

        Speaker(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public final Speaker speaker;
    public final String text;
    /** 消息在屏幕上的 Y 坐标（用于排序） */
    public final float yPosition;

    public ChatMessage(Speaker speaker, String text, float yPosition) {
        this.speaker = speaker;
        this.text = text;
        this.yPosition = yPosition;
    }

    @Override
    public String toString() {
        return speaker.getLabel() + ": " + text;
    }
}
