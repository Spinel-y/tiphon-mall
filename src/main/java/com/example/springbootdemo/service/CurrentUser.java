package com.example.springbootdemo.service;

import com.example.springbootdemo.entity.User;
import com.example.springbootdemo.repo.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class CurrentUser {

    private final UserRepository users;

    public CurrentUser(UserRepository users) {
        this.users = users;
    }

    /** 必须已登录，否则 401 */
    public User require(Authentication auth) {
        User user = optional(auth);
        if (user == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "请先登录");
        }
        return user;
    }

    /** 已登录就返回用户，游客返回 null */
    public User optional(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return users.findByUsername(auth.getName()).orElse(null);
    }

    public boolean isAdmin(Authentication auth) {
        User user = optional(auth);
        return user != null && "ADMIN".equals(user.getRole());
    }
}