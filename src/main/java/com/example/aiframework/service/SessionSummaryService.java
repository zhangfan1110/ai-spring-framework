package com.example.aiframework.service;

import com.example.aiframework.entity.ChatMessageEntity;
import com.example.aiframework.entity.ChatSessionSummaryEntity;
import com.example.aiframework.repository.ChatMessageRepository;
import com.example.aiframework.repository.ChatSessionSummaryRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会话摘要服务 - 为长对话生成摘要
 */
@Service
public class SessionSummaryService {
    
    private static final Logger log = LoggerFactory.getLogger(SessionSummaryService.class);
    
    @Autowired
    private ChatLanguageModel chatModel;
    
    @Autowired
    private ChatSessionSummaryRepository summaryRepository;
    
    @Autowired
    private ChatMessageRepository messageRepository;
    
    @Value("${chat.summary.auto-generate:true}")
    private boolean autoGenerate;
    
    @Value("${chat.summary.message-threshold:20}")
    private int messageThreshold;
    
    /**
     * 生成会话摘要
     */
    public ChatSessionSummaryEntity generateSummary(String sessionId) {
        List<ChatMessageEntity> messages = messageRepository.findBySessionId(sessionId);
        
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        
        // 检查是否已有摘要
        ChatSessionSummaryEntity existing = summaryRepository.findBySessionId(sessionId);
        if (existing != null) {
            // 如果消息数量没有显著增加，返回现有摘要
            int newMessages = messages.size() - getMessageCount(existing);
            if (newMessages < 5) {
                return existing;
            }
        }
        
        try {
            // 提取对话内容
            String conversation = messages.stream()
                .map(m -> m.getRole() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));
            
            // 调用 AI 生成摘要
            String prompt = String.format(
                "请总结以下对话内容，包括：\n" +
                "1. 主要讨论话题\n" +
                "2. 关键信息点\n" +
                "3. 达成的结论或待办事项\n\n" +
                "对话内容：\n%s\n\n" +
                "请分别返回摘要和关键点（JSON 格式）。",
                conversation.substring(0, Math.min(conversation.length(), 8000))
            );
            
            String aiResponse = chatModel.generate(prompt);
            
            // 解析 AI 响应 (简化处理，实际应该解析 JSON)
            String summary = aiResponse != null ? aiResponse.split("\n")[0] : "会话摘要";
            String keyPoints = extractKeyPoints(aiResponse);
            
            // 保存摘要
            ChatSessionSummaryEntity summaryEntity = ChatSessionSummaryEntity.builder()
                .sessionId(sessionId)
                .summary(summary)
                .keyPoints(keyPoints)
                .startMessageId(messages.get(0).getId())
                .endMessageId(messages.get(messages.size() - 1).getId())
                .build();
            
            summaryRepository.save(summaryEntity);
            
            log.info("生成会话摘要：{} - {} 条消息", sessionId, messages.size());
            return summaryEntity;
            
        } catch (Exception e) {
            log.error("生成会话摘要失败：{}", e.getMessage(), e);
            return existing;
        }
    }
    
    /**
     * 检查并自动生成摘要
     */
    public void checkAndAutoGenerate(String sessionId) {
        if (!autoGenerate) {
            return;
        }
        
        List<ChatMessageEntity> messages = messageRepository.findBySessionId(sessionId);
        if (messages != null && messages.size() >= messageThreshold) {
            generateSummary(sessionId);
        }
    }
    
    /**
     * 获取会话摘要
     */
    public ChatSessionSummaryEntity getSummary(String sessionId) {
        return summaryRepository.findBySessionId(sessionId);
    }
    
    /**
     * 从 AI 响应中提取关键点 (简化版)
     */
    private String extractKeyPoints(String aiResponse) {
        if (aiResponse == null) {
            return "[]";
        }
        // 简化处理，实际应该解析 JSON
        return "[\"关键信息待提取\"]";
    }
    
    /**
     * 获取摘要覆盖的消息数量
     */
    private int getMessageCount(ChatSessionSummaryEntity summary) {
        // 简化实现，实际应该查询数据库
        return 20;
    }
}
