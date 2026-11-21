package com.example.springbootdemo.controller;

import com.example.springbootdemo.entity.ChatPersona;
import com.example.springbootdemo.entity.User;
import com.example.springbootdemo.service.CurrentUser;
import com.example.springbootdemo.service.PersonaService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 会话页人设：读取、保存、恢复默认。
 * 只有登录用户能改，一人一份。
 */
@RestController
@RequestMapping("/api/chat")
public class PersonaController {

    private final PersonaService personas;
    private final CurrentUser currentUser;

    public PersonaController(PersonaService personas, CurrentUser currentUser) {
        this.personas = personas;
        this.currentUser = currentUser;
    }

    public record PersonaRequest(String preset, String botName, String userCall, String selfCall,
                                 String traits, String extra, String greeting) {
    }

    @GetMapping("/persona")
    public Map<String, Object> get(Authentication auth) {
        User user = currentUser.require(auth);
        return personas.view(user.getId());
    }

    @PutMapping("/persona")
    public Map<String, Object> save(Authentication auth, @RequestBody PersonaRequest body) {
        User user = currentUser.require(auth);
        try {
            ChatPersona saved = personas.save(user.getId(),
                    body.preset(), body.botName(), body.userCall(), body.selfCall(),
                    body.traits(), body.extra(), body.greeting());
            Map<String, Object> out = new LinkedHashMap<>(personas.view(user.getId()));
            out.put("message", "已换成「" + saved.getBotName() + "」，下一句话就生效喵~");
            return out;
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/persona/reset")
    public Map<String, Object> reset(Authentication auth) {
        User user = currentUser.require(auth);
        personas.reset(user.getId());
        Map<String, Object> out = new LinkedHashMap<>(personas.view(user.getId()));
        out.put("message", "已经恢复成默认人设啦~");
        return out;
    }
}