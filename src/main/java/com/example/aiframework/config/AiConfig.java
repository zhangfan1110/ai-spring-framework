package com.example.aiframework.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LangChain4j 配置类 - 通义千问 + 智谱 AI
 */
@Configuration
public class AiConfig {
    
    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);
    
    @Value("${langchain4j.qwen.api-key:}")
    private String qwenApiKey;
    
    @Value("${langchain4j.qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String qwenBaseUrl;
    
    @Value("${langchain4j.qwen.model-name:qwen3.5-plus}")
    private String qwenModelName;
    
    @Value("${langchain4j.qwen.temperature:0.7}")
    private Double qwenTemperature;




    /**
     * 通义千问 Chat Model
     * 使用 OpenAI 兼容接口
     */
    @Bean
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
     * 获取 Chat 模型名称
     */
    @Bean
    public String aiModelName() {
        return qwenModelName;
    }
}
