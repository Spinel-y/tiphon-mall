package com.example.springbootdemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 每个用户一份 AI 客服形象配置，主键就是 userId。
 */
@Entity
@Table(name = "cs_profiles")
public class CsProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "bot_name", length = 40)
    private String botName = "提丰小客服";

    @Column(name = "avatar_url", length = 300)
    private String avatarUrl;

    @Column(length = 1200)
    private String persona = "温柔、机灵、说话简短，偶尔用「喵」结尾，像店主家的小猫店员。";

    @Column(name = "theme_color", length = 20)
    private String themeColor = "#ff7a45";

    @Column(length = 200)
    private String greeting = "主人好呀，想找点什么？我可以帮你挑商品、看订单、聊聊天喵~";

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getBotName() {
        return botName;
    }

    public void setBotName(String botName) {
        this.botName = botName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getPersona() {
        return persona;
    }

    public void setPersona(String persona) {
        this.persona = persona;
    }

    public String getThemeColor() {
        return themeColor;
    }

    public void setThemeColor(String themeColor) {
        this.themeColor = themeColor;
    }

    public String getGreeting() {
        return greeting;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}