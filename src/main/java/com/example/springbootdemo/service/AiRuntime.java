package com.example.springbootdemo.service;

import com.example.springbootdemo.config.AiConfig;
import com.example.springbootdemo.config.CustomerAssistant;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.WanxImageModel;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 按当前 Key 懒创建模型实例。
 * 页面里保存新 Key 后，下一句话就会用新 Key，不需要重启应用。
 */
@Service
public class AiRuntime {

    private static final String DEEPSEEK_BASE_URL = "https://api.deepseek.com/v1";
    private static final String DEEPSEEK_MODEL = "deepseek-chat";
    private static final String DASHSCOPE_VISION_MODEL = "qwen3-vl-plus";
    private static final String DASHSCOPE_IMAGE_MODEL = "wan2.5-t2i-preview";

    private final ApiKeyStore keys;
    private final ToolsService toolsService;

    private volatile String builtFor;
    private volatile AiConfig.Assistant assistant;
    private volatile CustomerAssistant csAssistant;
    private volatile QwenChatModel visionModel;
    private volatile WanxImageModel imageModel;

    public AiRuntime(ApiKeyStore keys, ToolsService toolsService) {
        this.keys = keys;
        this.toolsService = toolsService;
    }

    private synchronized void ensureFresh() {
        String signature = keys.signature();
        if (signature.equals(builtFor)) {
            return;
        }

        assistant = null;
        csAssistant = null;
        visionModel = null;
        imageModel = null;

        if (keys.hasDeepseek()) {
            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                    .apiKey(keys.getDeepseekKey())
                    .baseUrl(DEEPSEEK_BASE_URL)
                    .modelName(DEEPSEEK_MODEL)
                    .timeout(Duration.ofSeconds(120))
                    .build();

            OpenAiStreamingChatModel streamingModel = OpenAiStreamingChatModel.builder()
                    .apiKey(keys.getDeepseekKey())
                    .baseUrl(DEEPSEEK_BASE_URL)
                    .modelName(DEEPSEEK_MODEL)
                    .timeout(Duration.ofSeconds(120))
                    .build();

            assistant = AiServices.builder(AiConfig.Assistant.class)
                    .chatLanguageModel(chatModel)
                    .streamingChatLanguageModel(streamingModel)
                    .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                            .id(memoryId)
                            .maxMessages(20)
                            .build())
                    .tools(toolsService)
                    .build();

            csAssistant = AiServices.builder(CustomerAssistant.class)
                    .chatLanguageModel(chatModel)
                    .streamingChatLanguageModel(streamingModel)
                    .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                            .id(memoryId)
                            .maxMessages(12)
                            .build())
                    .build();
        }

        if (keys.hasDashscope()) {
            visionModel = QwenChatModel.builder()
                    .apiKey(keys.getDashscopeKey())
                    .modelName(DASHSCOPE_VISION_MODEL)
                    .build();

            imageModel = WanxImageModel.builder()
                    .apiKey(keys.getDashscopeKey())
                    .modelName(DASHSCOPE_IMAGE_MODEL)
                    .build();
        }

        builtFor = signature;
    }

    public AiConfig.Assistant assistant() {
        ensureFresh();
        if (assistant == null) {
            throw new IllegalStateException("还没配置 DeepSeek 的 API Key，点左下角「设置」填一下再聊喵~");
        }
        return assistant;
    }

    public CustomerAssistant csAssistant() {
        ensureFresh();
        if (csAssistant == null) {
            throw new IllegalStateException("还没配置 DeepSeek 的 API Key，先让店主在会话页「设置」里填一下喵~");
        }
        return csAssistant;
    }

    public QwenChatModel visionModel() {
        ensureFresh();
        if (visionModel == null) {
            throw new IllegalStateException("还没配置 DashScope 的 API Key，点左下角「设置」填一下喵~");
        }
        return visionModel;
    }

    public WanxImageModel imageModel() {
        ensureFresh();
        if (imageModel == null) {
            throw new IllegalStateException("还没配置 DashScope 的 API Key，画不了图喵~");
        }
        return imageModel;
    }
}