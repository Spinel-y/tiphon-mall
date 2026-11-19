package com.example.springbootdemo.service;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.output.Response;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ToolsService {

    /** 用 ObjectProvider 取，避免和 AiRuntime 在构造期互相依赖 */
    @Autowired
    ObjectProvider<AiRuntime> aiRuntime;

    @Autowired
    ImageStorageService imageStorageService;

    @Tool("打开音乐播放器。仅当主人明确要求听歌、放音乐、打开播放器时调用，日常提及「" +
            "音乐」不要触发")
    String openMusicPlayer(){
        return "<!--NAV:MUSIC-->音乐播放器已打开，主人可以在左侧面板查看播放列表喵~";
    }

    @Tool("打开游戏中心。仅当主人明确要求玩游戏、打开游戏、小恐龙、贪吃蛇时调用，日常提及「游戏」「好无聊」不要触发")
    String openGameCenter(@P("游戏名称，可选值：dino(小恐龙)、snake(贪吃蛇)、all(全部)") String gameName){
        String game = (gameName != null && !gameName.isBlank()) ? gameName : "all";
        return "<!--NAV:GAMES:" + game + "-->游戏中心已打开，主人快来玩吧喵~";
    }

    @Tool("打开相册。仅当主人明确要求打开相册、查看相册时调用，日常提及「图片」「照片」不要触发")
    String openAlbum(){
        return "<!--NAV:ALBUM-->相册已打开，主人可以查看所有生成的图片喵~";
    }

    @Tool("根据文字描述生成一张图片，返回图片的本地URL")
    String generateImage(@P("图片描述") String description){
        try {
            Response<Image> response = aiRuntime.getObject().imageModel().generate(description);
            Image image = response.content();
            String filename;
            if (image.url() != null) {
                filename = imageStorageService.storeImageFromUrl(image.url().toString());
            } else if (image.base64Data() != null) {
                filename = imageStorageService.storeFromBase64(image.base64Data());
            } else {
                return "图片生成失败: 模型未返回图片数据";
            }
            return "<!--IMG:" + "/ai/images/" + filename + "-->" + description;
        } catch (Exception e) {
            return "图片生成失败: " + e.getMessage();
        }
    }
}