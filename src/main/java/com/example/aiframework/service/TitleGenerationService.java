package com.example.aiframework.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 标题生成服务 - 为会话自动生成标题
 */
@Service
public class TitleGenerationService {
    
    private static final Logger log = LoggerFactory.getLogger(TitleGenerationService.class);
    
    @Autowired
    private ChatLanguageModel chatModel;
    
    /**
     * 基于第一条消息生成标题
     */
    public String generateTitle(String firstMessage) {
        if (firstMessage == null || firstMessage.isEmpty()) {
            return "新会话";
        }
        
        // 简单方案：取前 20 个字
        if (firstMessage.length() <= 20) {
            return firstMessage;
        }
        
        // AI 智能总结 (异步调用，不阻塞主流程)
        try {
            String prompt = String.format(
                "请将以下用户消息总结为一个简短的标题（不超过 20 个字），只返回标题，不要其他内容：\n\n%s",
                firstMessage.substring(0, Math.min(firstMessage.length(), 200))
            );
            
            String aiTitle = chatModel.generate(prompt);
            if (aiTitle != null && !aiTitle.trim().isEmpty()) {
                return aiTitle.trim().replaceAll("[\"']", "");
            }
        } catch (Exception e) {
            log.warn("AI 生成标题失败，使用默认方案：{}", e.getMessage());
        }
        
        // 降级方案：截取前 20 个字
        return firstMessage.substring(0, 20) + "...";
    }
    
    /**
     * 基于多条消息生成会话摘要
     */
    public String generateSummary(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return "无内容";
        }
        
        try {
            String conversation = String.join("\n", messages.subList(0, Math.min(messages.size(), 10)));
            String prompt = String.format(
                "请将以下对话内容总结为一段简短的摘要（不超过 100 个字），只返回摘要，不要其他内容：\n\n%s",
                conversation
            );
            
            String summary = chatModel.generate(prompt);
            if (summary != null && !summary.trim().isEmpty()) {
                return summary.trim();
            }
        } catch (Exception e) {
            log.warn("AI 生成摘要失败：{}", e.getMessage());
        }
        
        return "会话包含 " + messages.size() + " 条消息";
    }
}
