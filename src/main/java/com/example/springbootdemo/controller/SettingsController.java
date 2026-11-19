package com.example.springbootdemo.controller;

import com.example.springbootdemo.service.ApiKeyStore;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 页面上的「设置」面板通过这两个接口读写 API Key。
 * Key 只以脱敏形式返回，完整值不会回到浏览器。
 */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final ApiKeyStore keyStore;

    public SettingsController(ApiKeyStore keyStore) {
        this.keyStore = keyStore;
    }

    @GetMapping("/keys")
    public Map<String, Object> status() {
        return statusPayload(false);
    }

    @PostMapping("/keys")
    public Map<String, Object> update(@RequestBody(required = false) Map<String, String> body) {
        String deepseek = body == null ? null : body.get("deepseekKey");
        String dashscope = body == null ? null : body.get("dashscopeKey");
        if ((deepseek == null || deepseek.isBlank()) && (dashscope == null || dashscope.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "没有填写任何 Key，没有需要保存的内容");
        }

        boolean changed;
        try {
            changed = keyStore.save(deepseek, dashscope);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "写不进 application-secret.properties：" + e.getMessage());
        }

        Map<String, Object> payload = statusPayload(true);
        payload.put("changed", changed);
        return payload;
    }

    private Map<String, Object> statusPayload(boolean saved) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("deepseek", keyStatus(keyStore.hasDeepseek(), keyStore.maskedDeepseek()));
        payload.put("dashscope", keyStatus(keyStore.hasDashscope(), keyStore.maskedDashscope()));
        payload.put("saved", saved);
        return payload;
    }

    private Map<String, Object> keyStatus(boolean configured, String masked) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("configured", configured);
        status.put("masked", masked);
        return status;
    }
}