package com.example.springbootdemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 会话页（提丰对话）的人设设置，一个用户一份。
 * 没存过就用预设的默认值，不会写库；用户点保存才会落库。
 */
@Entity
@Table(name = "chat_personas")
public class ChatPersona {

    /** 主键就是用户 id，一人一份 */
    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 预设 key：cat / assistant / coder */
    @Column(nullable = false, length = 32)
    private String preset = "cat";

    /** 机器人叫什么 */
    @Column(name = "bot_name", length = 40)
    private String botName;

    /** 怎么称呼用户 */
    @Column(name = "user_call", length = 40)
    private String userCall;

    /** 自称 */
    @Column(name = "self_call", length = 40)
    private String selfCall;

    /** 追加的性格描述 */
    @Column(length = 800)
    private String traits;

    /** 额外要求 */
    @Column(length = 800)
    private String extra;

    /** 开场白（聊天页欢迎气泡） */
    @Column(length = 300)
    private String greeting;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getPreset() { return preset; }
    public void setPreset(String preset) { this.preset = preset; }

    public String getBotName() { return botName; }
    public void setBotName(String botName) { this.botName = botName; }

    public String getUserCall() { return userCall; }
    public void setUserCall(String userCall) { this.userCall = userCall; }

    public String getSelfCall() { return selfCall; }
    public void setSelfCall(String selfCall) { this.selfCall = selfCall; }

    public String getTraits() { return traits; }
    public void setTraits(String traits) { this.traits = traits; }

    public String getExtra() { return extra; }
    public void setExtra(String extra) { this.extra = extra; }

    public String getGreeting() { return greeting; }
    public void setGreeting(String greeting) { this.greeting = greeting; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}