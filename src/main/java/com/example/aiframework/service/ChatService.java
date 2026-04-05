package com.example.aiframework.service;

import com.example.aiframework.entity.ChatMessageEntity;
import com.example.aiframework.entity.ChatSessionEntity;
import com.example.aiframework.model.ChatRequest;
import com.example.aiframework.model.ChatResponse;
import com.example.aiframework.repository.ChatMessageRepository;
import com.example.aiframework.repository.ChatSessionRepository;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * AI 聊天服务 - 带持久化记忆
 */
@Service
public class ChatService {
    
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    
    @Resource
    private ChatLanguageModel chatModel;
    
    @Autowired
    private ChatMessageRepository messageRepository;
    
    @Autowired
    private ChatSessionRepository sessionRepository;
    
    @Autowired(required = false)
    private RedisSessionCacheService redisSessionCacheService;
    
    @Autowired(required = false)
    private RagMemoryService ragMemoryService;
    
    @Autowired(required = false)
    private SessionSummaryService sessionSummaryService;
    
    @Value("${langchain4j.qwen.model-name:qwen-plus}")
    private String modelName;
    
    @Value("${chat.memory.window-size:10}")
    private int memoryWindowSize;
    
    @Value("${chat.memory.enabled:true}")
    private boolean memoryEnabled;
    
    @Value("${redis.cache.enabled:true}")
    private boolean redisCacheEnabled;
    
    /**
     * 聊天
     */
    public ChatResponse chat(ChatRequest request) {
        log.info("聊天请求 - sessionId: {}, message: {}", request.getSessionId(), request.getMessage());
        
        long startTime = System.currentTimeMillis();
        
        // 生成会话 ID
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
            request.setSessionId(sessionId);
        }
        
        // RAG 增强：检索相关记忆
        String query = request.getMessage();
        if (ragMemoryService != null) {
            List<String> relevantMemories = ragMemoryService.retrieveRelevantMemories(sessionId, query);
            if (!relevantMemories.isEmpty()) {
                query = ragMemoryService.buildRagPrompt(sessionId, query, relevantMemories);
            }
        }
        
        // 构建消息列表 (从数据库或缓存加载历史)
        List<dev.langchain4j.data.message.ChatMessage> messages = buildMessages(request, query);
        
        // 调用模型
        String response;
        if (chatModel != null) {
            dev.langchain4j.model.output.Response<AiMessage> aiResponse = chatModel.generate(messages);
            response = aiResponse.content().text();
        } else {
            response = "[演示模式] 我收到了您的消息：" + request.getMessage();
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        // 保存会话历史到数据库
        if (memoryEnabled) {
            saveSession(sessionId, messages, response, request.getSystemPrompt());
        }
        
        // 异步生成会话摘要 (不阻塞响应)
        if (sessionSummaryService != null) {
            final String finalSessionId = sessionId;
            new Thread(() -> sessionSummaryService.checkAndAutoGenerate(finalSessionId)).start();
        }
        
        log.info("聊天响应 - 耗时：{}ms, sessionId: {}", duration, sessionId);
        
        return ChatResponse.builder()
                .sessionId(sessionId)
                .content(response)
                .model(modelName)
                .duration(duration)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 构建消息列表
     */
    private List<dev.langchain4j.data.message.ChatMessage> buildMessages(ChatRequest request, String query) {
        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
        
        // 添加系统提示词
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            messages.add(SystemMessage.from(request.getSystemPrompt()));
        }
        
        // 添加历史消息 (从缓存或数据库加载)
        List<dev.langchain4j.data.message.ChatMessage> history = loadHistory(request.getSessionId());
        if (history != null) {
            messages.addAll(history);
        }
        
        // 添加当前用户消息 (可能已包含 RAG 增强的上下文)
        messages.add(UserMessage.from(query != null ? query : request.getMessage()));
        
        return messages;
    }
    
    /**
     * 加载会话历史
     */
    private List<dev.langchain4j.data.message.ChatMessage> loadHistory(String sessionId) {
        // 从 Redis 缓存加载
        if (redisCacheEnabled && redisSessionCacheService != null) {
            List<dev.langchain4j.data.message.ChatMessage> cached = redisSessionCacheService.getSessionHistory(sessionId);
            if (cached != null && !cached.isEmpty()) {
                return cached;
            }
        }
        
        // 从数据库加载
        if (memoryEnabled) {
            List<ChatMessageEntity> entities = messageRepository.findBySessionIdLimit(sessionId, memoryWindowSize);
            if (entities != null && !entities.isEmpty()) {
                List<dev.langchain4j.data.message.ChatMessage> history = entities.stream()
                    .map(this::toLangChainMessage)
                    .collect(Collectors.toList());
                
                // 保存到 Redis 缓存
                if (redisCacheEnabled && redisSessionCacheService != null) {
                    redisSessionCacheService.saveSessionHistory(sessionId, history);
                }
                
                return history;
            }
        }
        
        return new ArrayList<>();
    }
    
    /**
     * 保存会话历史
     */
    private void saveSession(String sessionId, List<dev.langchain4j.data.message.ChatMessage> messages, 
                            String response, String systemPrompt) {
        try {
            // 保存用户消息
            ChatMessageEntity userMessage = ChatMessageEntity.builder()
                    .sessionId(sessionId)
                    .role("USER")
                    .content(messages.get(messages.size() - 1).text())
                    .model(modelName)
                    .build();
            messageRepository.save(userMessage);
            
            // 保存 AI 回复
            ChatMessageEntity aiMessage = ChatMessageEntity.builder()
                    .sessionId(sessionId)
                    .role("AI")
                    .content(response)
                    .model(modelName)
                    .build();
            messageRepository.save(aiMessage);
            
            // RAG 索引：添加消息到向量数据库
            if (ragMemoryService != null) {
                ragMemoryService.indexMessage(userMessage);
                ragMemoryService.indexMessage(aiMessage);
            }
            
            // 更新会话元数据
            ChatSessionEntity session = sessionRepository.findById(sessionId);
            if (session == null) {
                // 创建新会话，自动生成标题
                String title = generateTitle(messages.get(messages.size() - 1).text());
                session = ChatSessionEntity.builder()
                        .sessionId(sessionId)
                        .title(title)
                        .messageCount(2)
                        .lastActiveTime(LocalDateTime.now())
                        .build();
            } else {
                session.setMessageCount(session.getMessageCount() + 2);
                session.setLastActiveTime(LocalDateTime.now());
            }
            sessionRepository.save(session);
            
            // 更新 Redis 缓存
            if (redisCacheEnabled && redisSessionCacheService != null) {
                List<dev.langchain4j.data.message.ChatMessage> history = loadHistory(sessionId);
                history.add(AiMessage.from(response));
                if (history.size() > memoryWindowSize) {
                    history = history.subList(history.size() - memoryWindowSize, history.size());
                }
                redisSessionCacheService.saveSessionHistory(sessionId, history);
            }
            
        } catch (Exception e) {
            log.error("保存会话失败：{}", e.getMessage(), e);
        }
    }
    

    
    /**
     * 转换实体为 LangChain4j 消息
     */
    private dev.langchain4j.data.message.ChatMessage toLangChainMessage(ChatMessageEntity entity) {
        switch (entity.getRole()) {
            case "SYSTEM":
                return SystemMessage.from(entity.getContent());
            case "AI":
                return AiMessage.from(entity.getContent());
            case "USER":
            default:
                return UserMessage.from(entity.getContent());
        }
    }
    
    /**
     * 生成会话标题 (使用 TitleGenerationService)
     */
    private String generateTitle(String firstMessage) {
        // 这里可以注入 TitleGenerationService，暂时使用简化方案
        if (firstMessage == null || firstMessage.isEmpty()) {
            return "新会话";
        }
        if (firstMessage.length() <= 20) {
            return firstMessage;
        }
        return firstMessage.substring(0, 20) + "...";
    }
    
    /**
     * 清空会话
     */
    public void clearSession(String sessionId) {
        if (memoryEnabled) {
            messageRepository.deleteBySessionId(sessionId);
            sessionRepository.deleteById(sessionId);
        }
        // 清空 Redis 缓存
        if (redisCacheEnabled && redisSessionCacheService != null) {
            redisSessionCacheService.deleteSession(sessionId);
        }
        log.info("清空会话：{}", sessionId);
    }
    
    /**
     * 获取会话历史
     */
    public List<ChatMessageEntity> getSessionHistory(String sessionId) {
        return messageRepository.findBySessionId(sessionId);
    }
    
    /**
     * 获取所有会话列表
     */
    public List<ChatSessionEntity> getAllSessions() {
        return sessionRepository.findAll();
    }
    
    /**
     * 获取最近会话
     */
    public List<ChatSessionEntity> getRecentSessions(int limit) {
        return sessionRepository.findRecentSessions(limit);
    }
    
    // ========== 流式输出 ==========
    
    /**
     * 流式聊天（SSE）- 模拟流式（逐字发送）
     * @param request 请求
     * @param contentConsumer 内容消费者（每收到一块内容就回调）
     * @return 完整的响应
     */
    public ChatResponse chatStream(ChatRequest request, Consumer<String> contentConsumer) {
        log.info("流式聊天请求 - sessionId: {}, message: {}", request.getSessionId(), request.getMessage());
        
        long startTime = System.currentTimeMillis();
        
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
            request.setSessionId(sessionId);
        }
        
        // RAG 增强
        String query = request.getMessage();
        if (ragMemoryService != null) {
            List<String> relevantMemories = ragMemoryService.retrieveRelevantMemories(sessionId, query);
            if (!relevantMemories.isEmpty()) {
                query = ragMemoryService.buildRagPrompt(sessionId, query, relevantMemories);
            }
        }
        
        // 构建消息列表
        List<dev.langchain4j.data.message.ChatMessage> messages = buildMessages(request, query);
        
        // 调用模型获取完整响应
        StringBuilder fullResponse = new StringBuilder();
        
        if (chatModel != null) {
            dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> aiResponse = chatModel.generate(messages);
            String response = aiResponse.content().text();
            fullResponse.append(response);
            
            // 模拟流式：逐字发送
            for (int i = 0; i < response.length(); i++) {
                String chunk = response.substring(i, Math.min(i + 3, response.length()));
                contentConsumer.accept(chunk);
                try {
                    Thread.sleep(20); // 模拟打字效果
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } else {
            String demoResponse = "[演示模式] 我收到了您的消息：" + request.getMessage();
            fullResponse.append(demoResponse);
            contentConsumer.accept(demoResponse);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        // 保存会话
        if (memoryEnabled) {
            saveSession(sessionId, messages, fullResponse.toString(), request.getSystemPrompt());
        }
        
        log.info("流式聊天完成 - 耗时：{}ms, sessionId: {}", duration, sessionId);
        
        return ChatResponse.builder()
                .sessionId(sessionId)
                .content(fullResponse.toString())
                .model(modelName)
                .duration(duration)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    // ========== 消息编辑删除 ==========
    
    /**
     * 编辑消息
     */
    public ChatMessageEntity editMessage(String messageId, String newContent) {
        ChatMessageEntity message = messageRepository.findById(messageId);
        if (message == null) {
            throw new RuntimeException("消息不存在：" + messageId);
        }
        
        message.setContent(newContent);
        message.setEdited(true);
        message.setEditedAt(LocalDateTime.now());
        
        return messageRepository.save(message);
    }
    
    /**
     * 删除消息
     */
    public void deleteMessage(String messageId) {
        ChatMessageEntity message = messageRepository.findById(messageId);
        if (message == null) {
            throw new RuntimeException("消息不存在：" + messageId);
        }
        
        // 软删除
        message.setDeleted(true);
        messageRepository.save(message);
        
        log.info("删除消息：{}", messageId);
    }
    
    /**
     * 重新生成 AI 回复
     */
    public ChatResponse regenerateResponse(String messageId) {
        ChatMessageEntity userMessage = messageRepository.findById(messageId);
        if (userMessage == null) {
            throw new RuntimeException("消息不存在：" + messageId);
        }
        
        if (!"USER".equals(userMessage.getRole())) {
            throw new RuntimeException("只能重新生成用户消息对应的 AI 回复");
        }
        
        // 查找该用户消息之后的 AI 回复
        List<ChatMessageEntity> messages = messageRepository.findBySessionId(userMessage.getSessionId());
        ChatMessageEntity aiMessageToDelete = null;
        for (ChatMessageEntity msg : messages) {
            if (msg.getId().equals(messageId)) {
                // 找到下一条 AI 消息
                int idx = messages.indexOf(msg) + 1;
                if (idx < messages.size() && "AI".equals(messages.get(idx).getRole())) {
                    aiMessageToDelete = messages.get(idx);
                }
                break;
            }
        }
        
        // 删除旧的 AI 回复
        if (aiMessageToDelete != null) {
            aiMessageToDelete.setDeleted(true);
            messageRepository.save(aiMessageToDelete);
        }
        
        // 重新生成
        ChatRequest request = new ChatRequest();
        request.setSessionId(userMessage.getSessionId());
        request.setMessage(userMessage.getContent());
        
        return chat(request);
    }
}
