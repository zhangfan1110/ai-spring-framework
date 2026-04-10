package com.example.aiframework.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * LangChain4j 配置类 - 通义千问 + 小米 MiMo
 */
@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    // ========== 通义千问配置 ==========
    @Value("${langchain4j.qwen.api-key:}")
    private String qwenApiKey;

    @Value("${langchain4j.qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String qwenBaseUrl;

    @Value("${langchain4j.qwen.model-name:qwen3.5-plus}")
    private String qwenModelName;

    @Value("${langchain4j.qwen.temperature:0.7}")
    private Double qwenTemperature;

    // ========== 小米 MiMo v2 Pro 配置 ==========
    @Value("${langchain4j.mimo.api-key:}")
    private String mimoApiKey;

    @Value("${langchain4j.mimo.base-url:https://api.xiaoai.mi.com/v1}")
    private String mimoBaseUrl;

    @Value("${langchain4j.mimo.model-name:MiMo-v2-Pro}")
    private String mimoModelName;

    @Value("${langchain4j.mimo.temperature:0.7}")
    private Double mimoTemperature;

    /**
     * 通义千问 Chat Model (默认模型)
     * 使用 OpenAI 兼容接口
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "langchain4j.qwen", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ChatLanguageModel qwenChatModel() {
        log.info("初始化通义千问 Chat Model: {}, baseUrl: {}", qwenModelName, qwenBaseUrl);

        return OpenAiChatModel.builder()
                .apiKey(qwenApiKey)
                .baseUrl(qwenBaseUrl)
                .modelName(qwenModelName)
                .temperature(qwenTemperature)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 小米 MiMo v2 Pro Chat Model
     * 使用 OpenAI 兼容接口
     */
    @Bean
    @ConditionalOnProperty(prefix = "langchain4j.mimo", name = "enabled", havingValue = "true")
    public ChatLanguageModel mimoChatModel() {
        log.info("初始化小米 MiMo Chat Model: {}, baseUrl: {}", mimoModelName, mimoBaseUrl);

        return OpenAiChatModel.builder()
                .apiKey(mimoApiKey)
                .baseUrl(mimoBaseUrl)
                .modelName(mimoModelName)
                .temperature(mimoTemperature)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 模型映射表 - 按名称获取对应的 ChatLanguageModel
     * ChatService 通过此 Bean 实现多模型切换
     */
    @Bean
    public Map<String, ChatLanguageModel> chatModelMap(
            ChatLanguageModel qwenChatModel,
            List<ChatLanguageModel> allModels) {
        Map<String, ChatLanguageModel> map = new HashMap<>();
        map.put("qwen", qwenChatModel);
        map.put(qwenModelName, qwenChatModel);
        // 自动发现并注册所有 ChatLanguageModel Bean
        for (ChatLanguageModel model : allModels) {
            if (model != qwenChatModel) {
                map.put("mimo", model);
                map.put(mimoModelName, model);
            }
        }
        return map;
    }

    /**
     * 获取默认 Chat 模型名称
     */
    @Bean
    public String aiModelName() {
        return qwenModelName;
    }
}
