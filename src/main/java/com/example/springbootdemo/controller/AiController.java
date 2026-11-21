package com.example.springbootdemo.controller;

import com.example.springbootdemo.config.AiConfig;
import com.example.springbootdemo.entity.ChatPersona;
import com.example.springbootdemo.entity.User;
import com.example.springbootdemo.service.AiRuntime;
import com.example.springbootdemo.service.CurrentUser;
import com.example.springbootdemo.service.ImageStorageService;
import com.example.springbootdemo.service.PersonaService;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.TokenStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    AiRuntime aiRuntime;

    @Autowired
    ImageStorageService imageStorageService;

    @Autowired
    PersonaService personaService;

    @Autowired
    CurrentUser currentUser;

    @GetMapping(value = "memory_stream_chat", produces = "text/stream;charset=utf-8")
    public Flux<String> memoryStreamChat(@RequestParam(defaultValue = "你是谁") String message,
                                         @RequestParam(required = false) Integer userId,
                                         Authentication auth) {
        AiConfig.Assistant assistant;
        try {
            assistant = aiRuntime.assistant();
        } catch (IllegalStateException e) {
            // 没配 Key 时别扔 500，给前端一句能看懂的话（400 + message）
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        // 已登录就用会话里的真实用户，人设也按这个用户取；未登录退回 URL 上的 userId（游客）
        User loginUser = currentUser.optional(auth);
        long personaUserId = loginUser != null ? loginUser.getId() : (userId != null ? userId : 0L);
        int memoryId = (int) personaUserId;
        ChatPersona persona = personaService.effective(personaUserId);
        String systemPrompt = personaService.systemPrompt(persona);

        TokenStream stream = assistant.stream(memoryId, systemPrompt, message);
        return Flux.create(sink -> {
            stream.onPartialResponse(s -> sink.next(s))
                    .onCompleteResponse(c -> sink.complete())
                    .onError(sink::error)
                    .start();
        });
    }

    @PostMapping("/upload")
    public Map<String, String> uploadImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(defaultValue = "") String message) throws IOException {

        String filename = imageStorageService.storeUploadedImage(image);
        String imageUrl = "/ai/images/" + filename;

        String base64 = Base64.getEncoder().encodeToString(image.getBytes());
        String mimeType = image.getContentType();
        if (mimeType == null) {
            mimeType = "image/jpeg";
        }

        UserMessage userMessage = UserMessage.from(
                TextContent.from("请详细描述这张图片的内容，用中文回答"),
                ImageContent.from(base64, mimeType)
        );

        String description;
        try {
            description = aiRuntime.visionModel().chat(userMessage).aiMessage().text();
        } catch (Exception e) {
            description = "（无法描述图片内容）";
        }

        return Map.of(
                "filename", filename,
                "imageUrl", imageUrl,
                "description", description
        );
    }

    @GetMapping("/images/{filename}")
    public void serveImage(@PathVariable String filename, HttpServletResponse response) throws IOException {
        Path imagePath = imageStorageService.resolve(filename);
        if (!Files.exists(imagePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String contentType = Files.probeContentType(imagePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        response.setContentType(contentType);
        Files.copy(imagePath, response.getOutputStream());
    }

    @GetMapping("/images/list")
    public java.util.List<String> listImages() throws IOException {
        return imageStorageService.listImages();
    }

}