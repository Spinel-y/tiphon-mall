package com.example.springbootdemo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 站点入口：打开 http://localhost:8899/ 直接进购物页（公开页面），
 * 右上角「登录」走主人验证，验证通过后才进会话页。
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/gouwupingtai.html";
    }
}