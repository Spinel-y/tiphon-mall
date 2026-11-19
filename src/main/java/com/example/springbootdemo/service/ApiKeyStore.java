package com.example.springbootdemo.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * 两个 API Key 的唯一存放处。
 * 启动时从项目根目录的 application-secret.properties 读取，
 * 页面里填写后写回同一个文件，重启后依然有效。
 */
@Service
public class ApiKeyStore {

    private static final Path SECRET_FILE = Paths.get("application-secret.properties");
    private static final String DEEPSEEK_PROPERTY = "langchain4j.open-ai.chat-model.api-key";
    private static final String STREAMING_PROPERTY = "langchain4j.open-ai.streaming-chat-model.api-key";
    private static final String DASHSCOPE_PROPERTY = "dashscope.api-key";

    private volatile String deepseekKey = "";
    private volatile String dashscopeKey = "";

    @PostConstruct
    void load() {
        try {
            if (Files.isRegularFile(SECRET_FILE)) {
                Properties props = new Properties();
                try (InputStream in = Files.newInputStream(SECRET_FILE)) {
                    props.load(in);
                }
                deepseekKey = clean(props.getProperty(DEEPSEEK_PROPERTY));
                dashscopeKey = clean(props.getProperty(DASHSCOPE_PROPERTY));
            }
        } catch (IOException ignored) {
            // 文件读不了就按“未配置”处理，页面里重新填一次即可
        }
        if (deepseekKey.isEmpty()) {
            deepseekKey = clean(System.getenv("DEEPSEEK_API_KEY"));
        }
        if (dashscopeKey.isEmpty()) {
            dashscopeKey = clean(System.getenv("DASHSCOPE_API_KEY"));
        }
    }

    /**
     * 保存新 Key：传空值表示“这个不动”，只改填了的那一个。
     *
     * @return 是否真的有改动
     */
    public synchronized boolean save(String deepseek, String dashscope) throws IOException {
        String nextDeepseek = isBlank(deepseek) ? deepseekKey : deepseek.trim();
        String nextDashscope = isBlank(dashscope) ? dashscopeKey : dashscope.trim();
        boolean changed = !nextDeepseek.equals(deepseekKey) || !nextDashscope.equals(dashscopeKey);

        writeFile(nextDeepseek, nextDashscope);
        deepseekKey = nextDeepseek;
        dashscopeKey = nextDashscope;
        return changed;
    }

    private void writeFile(String deepseek, String dashscope) throws IOException {
        // 先把文件里已有的其它配置（数据库账号密码等）读出来，再覆盖 Key 三项，
        // 否则在页面里点一次「保存」就会把数据库配置冲掉。
        Properties props = new Properties();
        if (Files.isRegularFile(SECRET_FILE)) {
            try (InputStream in = Files.newInputStream(SECRET_FILE)) {
                props.load(in);
            }
        }
        props.setProperty(DEEPSEEK_PROPERTY, deepseek);
        props.setProperty(STREAMING_PROPERTY, "${" + DEEPSEEK_PROPERTY + "}");
        props.setProperty(DASHSCOPE_PROPERTY, dashscope);

        Path tmp = SECRET_FILE.resolveSibling(SECRET_FILE.getFileName() + ".tmp");
        try (java.io.Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            props.store(writer, "提丰配置（设置面板写入，其它项请手动改）");
        }
        try {
            Files.move(tmp, SECRET_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(tmp, SECRET_FILE, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** 用于判断“Key 有没有变”，AiRuntime 拿它决定要不要重建模型 */
    public String signature() {
        return deepseekKey + '\u0000' + dashscopeKey;
    }

    public String getDeepseekKey() {
        return deepseekKey;
    }

    public String getDashscopeKey() {
        return dashscopeKey;
    }

    public boolean hasDeepseek() {
        return !deepseekKey.isEmpty();
    }

    public boolean hasDashscope() {
        return !dashscopeKey.isEmpty();
    }

    /** 只返回前后几位，页面上显示“已配置”用，绝不把完整 Key 吐回浏览器 */
    public String maskedDeepseek() {
        return mask(deepseekKey);
    }

    public String maskedDashscope() {
        return mask(dashscopeKey);
    }

    private static String mask(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        if (key.length() <= 12) {
            return key.substring(0, 3) + "****";
        }
        return key.substring(0, 6) + "****" + key.substring(key.length() - 4);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}