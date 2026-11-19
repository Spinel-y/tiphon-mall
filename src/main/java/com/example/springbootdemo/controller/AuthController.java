package com.example.springbootdemo.controller;

import com.example.springbootdemo.entity.User;
import com.example.springbootdemo.repo.UserRepository;
import com.example.springbootdemo.service.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Pattern USERNAME_OK = Pattern.compile("[A-Za-z0-9_\\u4e00-\\u9fa5]{3,20}");
    private static final Pattern PHONE_OK = Pattern.compile("\\d{6,20}");

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final SecurityContextRepository contextRepository;
    private final CurrentUser currentUser;

    public AuthController(UserRepository users, PasswordEncoder encoder,
                          SecurityContextRepository contextRepository, CurrentUser currentUser) {
        this.users = users;
        this.encoder = encoder;
        this.contextRepository = contextRepository;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body,
                                        HttpServletRequest request, HttpServletResponse response) {
        String username = text(body.get("username"));
        String password = body.getOrDefault("password", "");
        String phone = text(body.get("phone"));
        String nickname = text(body.get("nickname"));

        if (!USERNAME_OK.matcher(username).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名要 3-20 位，只能用中英文、数字、下划线");
        }
        if (password.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码至少 6 位");
        }
        if (!phone.isEmpty() && !PHONE_OK.matcher(phone).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "手机号格式不太对");
        }
        if (users.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "这个用户名已经有人用了，换一个喵");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(password));
        user.setPhone(phone.isEmpty() ? null : phone);
        user.setNickname(nickname.isEmpty() ? username : nickname);
        user.setRole("USER");
        users.save(user);

        signIn(user, request, response);
        return Map.of("user", view(user));
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body,
                                     HttpServletRequest request, HttpServletResponse response) {
        String username = text(body.get("username"));
        String password = body.getOrDefault("password", "");

        User user = users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码不对喵"));
        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码不对喵");
        }
        if (user.getNickname() == null || user.getNickname().isBlank()) {
            user.setNickname(user.getUsername());
        }

        signIn(user, request, response);
        return Map.of("user", view(user));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return Map.of("ok", true);
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        return view(currentUser.require(auth));
    }

    /** 登录后把认证信息放进 session，浏览器带 cookie 就能保持登录 */
    private void signIn(User user, HttpServletRequest request, HttpServletResponse response) {
        UserDetails details = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole())
                .build();
        var authentication = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true);
        contextRepository.saveContext(context, request, response);
    }

    private static Map<String, Object> view(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("nickname", user.getNickname() == null ? user.getUsername() : user.getNickname());
        map.put("avatarUrl", user.getAvatarUrl());
        map.put("phone", user.getPhone());
        map.put("role", user.getRole());
        return map;
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }
}