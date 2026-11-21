package com.example.springbootdemo.config;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 提丰的对话接口。
 * 人设不再写死在这里：system prompt 由 PersonaService 按用户设置拼好后逐次传入，
 * 所以用户在「人设」里改完，下一句话就生效，不用重启也不用重建模型。
 * 模型实例（chat / streaming / vision / image）由 AiRuntime 按当前 Key 动态创建。
 */
public class AiConfig {

    public interface Assistant {

        @SystemMessage("{{systemPrompt}}")
        String chat(@MemoryId int memoryId,
                    @V("systemPrompt") String systemPrompt,
                    @UserMessage String message);

        @SystemMessage("{{systemPrompt}}")
        TokenStream stream(@MemoryId int memoryId,
                           @V("systemPrompt") String systemPrompt,
                           @UserMessage String message);
    }
}