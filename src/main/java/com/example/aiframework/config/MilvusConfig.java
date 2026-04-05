package com.example.aiframework.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * Milvus 向量数据库配置
 */
@Configuration
public class MilvusConfig {
    
    private static final Logger log = LoggerFactory.getLogger(MilvusConfig.class);
    
    @Value("${milvus.host:localhost}")
    private String host;
    
    @Value("${milvus.port:19530}")
    private Integer port;
    
    @Value("${milvus.username:admin}")
    private String username;
    
    @Value("${milvus.password:sdlkg007}")
    private String password;
    
    @Value("${milvus.database:ai-spring-framework}")
    private String database;


    @Value("${langchain4j.zhipu.api-key:}")
    private String zhipuApiKey;

    @Value("${langchain4j.zhipu.base-url:https://open.bigmodel.cn/api/paas/v4}")
    private String zhipuBaseUrl;

    @Value("${langchain4j.zhipu.embedding-model:text_embedding}")
    private String zhipuEmbeddingModel;


    /**
     * LangChain4j Milvus Embedding Store
     */
    @Bean
    @ConditionalOnProperty(prefix = "milvus", name = "enabled", havingValue = "true", matchIfMissing = false)
    public dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore milvusEmbeddingStore() {
        log.info("初始化 Milvus Embedding Store: {}:{}, database: {}", host, port, database);
        log.warn("注意：Milvus 默认文本字段长度限制为 36 字符，请确保文档分片长度不超过此限制");
        log.warn("解决方案：在 MilvusService.addDocumentWithSplitting() 中设置较小的 chunkSize (建议 30)");
        
        try {
            // 使用 builder 方式创建（LangChain4j 0.30.0）
            var builder = dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore.builder()
                    .host(host)
                    .port(port)
                    .databaseName(database)
                    .username(username)
                    .password(password)
                    .collectionName("default_collection")
                    .dimension(1024);
            
            var store = builder.build();
            
            log.info("成功连接到 Milvus: {}:{}", host, port);
            return store;
            
        } catch (Exception e) {
            log.error("Milvus 连接失败：{}", e.getMessage(), e);
            return null;
        }
    }


    /**
     * 智谱 AI Embedding Model
     * 用于向量化（Milvus 需要）
     * 模型：text_embedding (1024 维)
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "langchain4j.zhipu", name = "enabled", havingValue = "true", matchIfMissing = false)
    public EmbeddingModel zhipuEmbeddingModel() {
        log.info("初始化智谱 AI Embedding Model: {}, baseUrl: {}", zhipuEmbeddingModel, zhipuBaseUrl);

        return OpenAiEmbeddingModel.builder()
                .apiKey(zhipuApiKey)
                .baseUrl(zhipuBaseUrl)
                .modelName(zhipuEmbeddingModel)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
