package com.example.springbootdemo.service;

import com.example.springbootdemo.entity.ChatPersona;
import com.example.springbootdemo.repo.ChatPersonaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 会话页人设：读写用户设置、拼出最终的 system prompt。
 * 每句话都重新取一次，所以用户保存后下一句话就生效，不用重启。
 */
@Service
public class PersonaService {

    private static final int MAX_NAME = 40;
    private static final int MAX_TEXT = 800;
    private static final int MAX_GREETING = 300;
    private static final int MAX_PROMPT = 6000;

    private final ChatPersonaRepository repo;

    public PersonaService(ChatPersonaRepository repo) {
        this.repo = repo;
    }

    /** 当前用户的人设，没存过就返回预设默认值（不落库） */
    public ChatPersona effective(long userId) {
        return repo.findById(userId).orElseGet(() -> defaults(userId, PersonaPresets.defaultPreset()));
    }

    private ChatPersona defaults(long userId, PersonaPresets.Preset preset) {
        ChatPersona p = new ChatPersona();
        p.setUserId(userId);
        p.setPreset(preset.key());
        p.setBotName(defaultBotName(preset.key()));
        p.setUserCall(defaultUserCall(preset.key()));
        p.setSelfCall(defaultSelfCall(preset.key()));
        p.setGreeting(preset.greeting());
        return p;
    }

    private String defaultBotName(String presetKey) {
        return switch (presetKey) {
            case "assistant" -> "小助手";
            case "coder" -> "阿码";
            default -> "提丰";
        };
    }

    private String defaultUserCall(String presetKey) {
        return "cat".equals(presetKey) ? "主人" : "你";
    }

    private String defaultSelfCall(String presetKey) {
        return "cat".equals(presetKey) ? "小提丰" : "我";
    }

    /** 前端要展示的一整包：当前设置 + 预设列表 + 拼好的提示词 */
    public Map<String, Object> view(long userId) {
        ChatPersona p = effective(userId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("preset", p.getPreset());
        out.put("botName", p.getBotName());
        out.put("userCall", p.getUserCall());
        out.put("selfCall", p.getSelfCall());
        out.put("traits", p.getTraits() == null ? "" : p.getTraits());
        out.put("extra", p.getExtra() == null ? "" : p.getExtra());
        out.put("greeting", p.getGreeting() == null ? "" : p.getGreeting());
        out.put("customized", repo.existsById(userId));
        out.put("presets", PersonaPresets.ALL.stream().map(pr -> Map.of(
                "key", pr.key(),
                "name", pr.name(),
                "desc", pr.desc(),
                "greeting", pr.greeting()
        )).toList());
        out.put("systemPrompt", systemPrompt(p));
        return out;
    }

    /** 保存；preset 必须是内置的几套之一 */
    @Transactional
    public ChatPersona save(long userId, String presetKey, String botName, String userCall,
                            String selfCall, String traits, String extra, String greeting) {
        PersonaPresets.Preset preset = PersonaPresets.byKey(presetKey);
        if (preset == null) {
            throw new IllegalArgumentException("没有这套人设：" + presetKey);
        }

        ChatPersona target = repo.findById(userId).orElseGet(() -> defaults(userId, preset));
        target.setUserId(userId);
        target.setPreset(preset.key());
        target.setBotName(trimTo(botName, MAX_NAME, defaultBotName(preset.key())));
        target.setUserCall(trimTo(userCall, MAX_NAME, defaultUserCall(preset.key())));
        target.setSelfCall(trimTo(selfCall, MAX_NAME, defaultSelfCall(preset.key())));
        target.setTraits(trimTo(traits, MAX_TEXT, null));
        target.setExtra(trimTo(extra, MAX_TEXT, null));
        target.setGreeting(trimTo(greeting, MAX_GREETING, preset.greeting()));
        target.setUpdatedAt(Instant.now());
        return repo.save(target);
    }

    /** 恢复默认：删掉这条记录，再取就是预设值 */
    @Transactional
    public void reset(long userId) {
        if (repo.existsById(userId)) {
            repo.deleteById(userId);
        }
    }

    /** 拼最终 system prompt：身份 → 预设风格 → 补充设定 → 工具规则 → 日期 */
    public String systemPrompt(ChatPersona p) {
        PersonaPresets.Preset preset = PersonaPresets.byKey(p.getPreset());
        if (preset == null) {
            preset = PersonaPresets.defaultPreset();
        }
        String botName = blankTo(p.getBotName(), defaultBotName(preset.key()));
        String userCall = blankTo(p.getUserCall(), defaultUserCall(preset.key()));
        String selfCall = blankTo(p.getSelfCall(), defaultSelfCall(preset.key()));

        StringBuilder sb = new StringBuilder();
        sb.append("你是「").append(botName).append("」。");
        sb.append("称呼用户为「").append(userCall).append("」；自称「").append(selfCall).append("」");
        if (!"我".equals(selfCall)) {
            sb.append("，也可以直接说「我」");
        }
        sb.append("。\n\n").append(preset.prompt().trim());

        if (!isBlank(p.getTraits())) {
            sb.append("\n\n补充设定：").append(p.getTraits().trim());
        }
        if (!isBlank(p.getExtra())) {
            sb.append("\n额外要求：").append(p.getExtra().trim());
        }
        sb.append("\n\n").append(PersonaPresets.TOOLS);
        sb.append("\n今天的日期是 ").append(LocalDate.now()).append('。');

        String prompt = sb.toString();
        return prompt.length() > MAX_PROMPT ? prompt.substring(0, MAX_PROMPT) : prompt;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String blankTo(String s, String fallback) {
        return isBlank(s) ? fallback : s.trim();
    }

    private static String trimTo(String s, int max, String fallback) {
        if (isBlank(s)) {
            return fallback;
        }
        String v = s.trim();
        return v.length() > max ? v.substring(0, max) : v;
    }
}