package com.example.springbootdemo.controller;

import com.example.springbootdemo.entity.CsProfile;
import com.example.springbootdemo.entity.User;
import com.example.springbootdemo.repo.CsProfileRepository;
import com.example.springbootdemo.service.AiRuntime;
import com.example.springbootdemo.service.CurrentUser;
import com.example.springbootdemo.service.ImageStorageService;
import com.example.springbootdemo.service.ShopContextService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 客服：形象配置（名字/头像/人设/主题色/欢迎语）+ 流式对话。
 * 游客也能聊，只是没有个性化资料。
 */
@RestController
@RequestMapping("/api/cs")
public class CsController {

    private final CsProfileRepository profiles;
    private final CurrentUser currentUser;
    private final ShopContextService shopContext;
    private final AiRuntime aiRuntime;
    private final ImageStorageService images;

    public CsController(CsProfileRepository profiles, CurrentUser currentUser,
                        ShopContextService shopContext, AiRuntime aiRuntime, ImageStorageService images) {
        this.profiles = profiles;
        this.currentUser = currentUser;
        this.shopContext = shopContext;
        this.aiRuntime = aiRuntime;
        this.images = images;
    }

    @GetMapping("/profile")
    public Map<String, Object> profile(Authentication auth) {
        User user = currentUser.require(auth);
        return view(ensure(user.getId()));
    }

    @PutMapping("/profile")
    public Map<String, Object> update(@RequestBody Map<String, String> body, Authentication auth) {
        User user = currentUser.require(auth);
        CsProfile profile = ensure(user.getId());

        if (body.containsKey("botName")) {
            String name = trim(body.get("botName"));
            if (name.isEmpty() || name.length() > 20) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "客服名字 1-20 个字");
            }
            profile.setBotName(name);
        }
        if (body.containsKey("persona")) {
            String persona = trim(body.get("persona"));
            if (persona.length() > 600) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "人设描述最多 600 字");
            }
            profile.setPersona(persona.isEmpty() ? null : persona);
        }
        if (body.containsKey("greeting")) {
            profile.setGreeting(limit(trim(body.get("greeting")), 120));
        }
        if (body.containsKey("themeColor")) {
            String color = trim(body.get("themeColor"));
            if (!color.matches("#[0-9a-fA-F]{6}")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "主题色要写成 #ff7a45 这种格式");
            }
            profile.setThemeColor(color);
        }
        if (body.containsKey("avatarUrl")) {
            profile.setAvatarUrl(limit(trim(body.get("avatarUrl")), 300));
        }
        profile.setUpdatedAt(Instant.now());
        profiles.save(profile);
        return view(profile);
    }

    @PostMapping("/avatar")
    public Map<String, Object> avatar(@RequestParam("avatar") MultipartFile file, Authentication auth) throws IOException {
        User user = currentUser.require(auth);
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "没收到图片");
        }
        String filename = images.storeUploadedImage(file);
        CsProfile profile = ensure(user.getId());
        profile.setAvatarUrl("/ai/images/" + filename);
        profile.setUpdatedAt(Instant.now());
        profiles.save(profile);
        return view(profile);
    }

    @PostMapping("/chat")
    public Flux<String> chat(@RequestBody Map<String, String> body, Authentication auth, HttpServletRequest request) {
        String message = trim(body.get("message"));
        if (message.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "说点什么吧喵");
        }
        if (message.length() > 500) {
            message = message.substring(0, 500);
        }

        User user = currentUser.optional(auth);
        CsProfile profile = user == null ? defaults() : ensure(user.getId());
        String shop = shopContext.build(user);
        int memoryId = user != null ? user.getId().intValue() : ("guest:" + request.getSession(true).getId()).hashCode();

        var assistant = resolveAssistant();
        String finalMessage = message;
        return Flux.create(sink -> assistant
                .chat(memoryId, profile.getBotName(), profile.getPersona(), shop, finalMessage)
                .onPartialResponse(sink::next)
                .onCompleteResponse(response -> sink.complete())
                .onError(sink::error)
                .start());
    }

    private com.example.springbootdemo.config.CustomerAssistant resolveAssistant() {
        try {
            return aiRuntime.csAssistant();
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private CsProfile ensure(Long userId) {
        return profiles.findById(userId).orElseGet(() -> {
            CsProfile profile = defaults();
            profile.setUserId(userId);
            return profiles.save(profile);
        });
    }

    private static CsProfile defaults() {
        return new CsProfile();
    }

    private static Map<String, Object> view(CsProfile profile) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("botName", profile.getBotName());
        map.put("avatarUrl", profile.getAvatarUrl());
        map.put("persona", profile.getPersona());
        map.put("themeColor", profile.getThemeColor());
        map.put("greeting", profile.getGreeting());
        return map;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String limit(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}