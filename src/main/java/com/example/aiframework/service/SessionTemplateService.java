package com.example.aiframework.service;

import com.example.aiframework.entity.ChatSessionEntity;
import com.example.aiframework.entity.ChatSessionTemplateEntity;
import com.example.aiframework.repository.ChatSessionRepository;
import com.example.aiframework.repository.ChatSessionTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 会话模板服务
 */
@Service
public class SessionTemplateService {
    
    private static final Logger log = LoggerFactory.getLogger(SessionTemplateService.class);
    
    @Autowired(required = false)
    private ChatSessionTemplateRepository templateRepository;
    
    @Autowired(required = false)
    private ChatSessionRepository sessionRepository;
    
    @PostConstruct
    public void initBuiltInTemplates() {
        if (templateRepository == null) {
            return;
        }
        
        // 检查是否已有内置模板
        List<ChatSessionTemplateEntity> existing = templateRepository.findBuiltInTemplates();
        if (!existing.isEmpty()) {
            return; // 已初始化
        }
        
        // 创建内置模板
        createBuiltInTemplate("english-tutor", "英语外教", "learning", "👨‍🏫",
            "You are a friendly English tutor. Help the user practice English conversation. " +
            "Correct their grammar mistakes gently and explain in simple terms. " +
            "Keep conversations natural and engaging.",
            "Hello! I'm your English tutor. What would you like to talk about today?",
            Arrays.asList("日常对话", "语法纠正", "词汇扩展"));
        
        createBuiltInTemplate("code-reviewer", "代码审查专家", "coding", "👨‍💻",
            "You are an experienced code reviewer. Analyze the code provided and give constructive feedback on:\n" +
            "1. Code quality and best practices\n" +
            "2. Potential bugs or issues\n" +
            "3. Performance considerations\n" +
            "4. Security concerns\n" +
            "5. Suggestions for improvement\n\n" +
            "Be specific and provide code examples when possible.",
            "Hi! I'm ready to review your code. Please share the code you'd like me to examine.",
            Arrays.asList("代码优化", "Bug 检查", "安全审计"));
        
        createBuiltInTemplate("writing-assistant", "写作助手", "writing", "✍️",
            "You are a professional writing assistant. Help the user improve their writing by:\n" +
            "1. Checking grammar and spelling\n" +
            "2. Improving clarity and flow\n" +
            "3. Suggesting better word choices\n" +
            "4. Maintaining the user's voice and style\n\n" +
            "Provide explanations for your suggestions.",
            "Hello! I'm here to help with your writing. What would you like me to review?",
            Arrays.asList("文章润色", "语法检查", "风格优化"));
        
        createBuiltInTemplate("data-analyst", "数据分析师", "analysis", "📊",
            "You are a data analyst expert. Help the user understand and analyze data by:\n" +
            "1. Explaining statistical concepts clearly\n" +
            "2. Suggesting appropriate analysis methods\n" +
            "3. Interpreting results and findings\n" +
            "4. Recommending visualizations\n\n" +
            "Use examples and avoid jargon when possible.",
            "Hi! I'm your data analysis assistant. What data are you working with?",
            Arrays.asList("统计分析", "数据可视化", "趋势分析"));
        
        createBuiltInTemplate("roleplay-interview", "面试模拟", "roleplay", "🎭",
            "You are conducting a job interview for the position of {position}. " +
            "Ask relevant interview questions one at a time, evaluate the candidate's responses, " +
            "and provide feedback. Be professional but friendly.\n\n" +
            "After the interview, give an overall assessment and suggestions for improvement.",
            "Welcome to the interview! I'm excited to learn about you. Let's get started - could you tell me about yourself?",
            Arrays.asList("技术面试", "行为面试", "薪资谈判"));
        
        createBuiltInTemplate("travel-guide", "旅行规划师", "other", "✈️",
            "You are an experienced travel planner. Help users plan their trips by:\n" +
            "1. Suggesting destinations based on preferences\n" +
            "2. Creating detailed itineraries\n" +
            "3. Providing local tips and insights\n" +
            "4. Recommending budget-friendly options\n\n" +
            "Ask about their interests, budget, and travel dates.",
            "Hello! I'm your travel planner. Where are you dreaming of going?",
            Arrays.asList("行程规划", "预算建议", "当地美食"));
        
        log.info("内置模板初始化完成，共 {} 个", 6);
    }
    
    /**
     * 创建内置模板
     */
    private void createBuiltInTemplate(String templateId, String name, String category, String icon,
                                       String systemPrompt, String openingMessage, List<String> suggestedQuestions) {
        ChatSessionTemplateEntity template = new ChatSessionTemplateEntity();
        template.setTemplateId(templateId);
        template.setTemplateName(name);
        template.setCategory(category);
        template.setIcon(icon);
        template.setSystemPrompt(systemPrompt);
        template.setOpeningMessage(openingMessage);
        template.setSuggestedQuestions(String.join("|", suggestedQuestions));
        template.setIsBuiltIn(true);
        template.setCreateTime(LocalDateTime.now());
        template.setDisabled(false);
        
        templateRepository.save(template);
    }
    
    /**
     * 获取所有模板
     */
    public List<ChatSessionTemplateEntity> getAllTemplates() {
        return templateRepository != null ? templateRepository.findAll() : List.of();
    }
    
    /**
     * 根据 ID 获取模板
     */
    public ChatSessionTemplateEntity getTemplateById(String templateId) {
        return templateRepository != null ? templateRepository.findById(templateId) : null;
    }
    
    /**
     * 按分类获取模板
     */
    public List<ChatSessionTemplateEntity> getTemplatesByCategory(String category) {
        return templateRepository != null ? templateRepository.findByCategory(category) : List.of();
    }
    
    /**
     * 搜索模板
     */
    public List<ChatSessionTemplateEntity> searchTemplates(String keyword) {
        return templateRepository != null ? templateRepository.search(keyword) : List.of();
    }
    
    /**
     * 使用模板创建会话
     */
    public ChatSessionEntity createSessionFromTemplate(String templateId, String creatorId) {
        ChatSessionTemplateEntity template = getTemplateById(templateId);
        if (template == null || template.getDisabled()) {
            return null;
        }
        
        // 创建会话
        ChatSessionEntity session = new ChatSessionEntity();
        session.setSessionId(UUID.randomUUID().toString());
        session.setTitle(template.getTemplateName());
        session.setMessageCount(0);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        session.setLastActiveTime(LocalDateTime.now());
        
        if (sessionRepository != null) {
            sessionRepository.save(session);
        }
        
        // 增加模板使用次数
        if (templateRepository != null) {
            templateRepository.incrementUsageCount(templateId);
        }
        
        log.info("使用模板创建会话 - templateId: {}, sessionId: {}", templateId, session.getSessionId());
        return session;
    }
    
    /**
     * 创建自定义模板
     */
    public ChatSessionTemplateEntity createTemplate(String name, String description, String category,
                                                     String systemPrompt, String creatorId) {
        ChatSessionTemplateEntity template = new ChatSessionTemplateEntity();
        template.setTemplateId(UUID.randomUUID().toString());
        template.setTemplateName(name);
        template.setDescription(description);
        template.setCategory(category);
        template.setSystemPrompt(systemPrompt);
        template.setCreatorId(creatorId);
        template.setIsBuiltIn(false);
        template.setCreateTime(LocalDateTime.now());
        template.setDisabled(false);
        
        if (templateRepository != null) {
            templateRepository.save(template);
        }
        
        log.info("创建自定义模板 - name: {}, creator: {}", name, creatorId);
        return template;
    }
    
    /**
     * 删除模板
     */
    public void deleteTemplate(String templateId, String creatorId) {
        ChatSessionTemplateEntity template = getTemplateById(templateId);
        if (template == null) {
            return;
        }
        
        // 内置模板不能删除
        if (template.getIsBuiltIn()) {
            log.warn("不能删除内置模板 - templateId: {}", templateId);
            return;
        }
        
        // 只能删除自己的模板
        if (!creatorId.equals(template.getCreatorId())) {
            log.warn("无权删除他人的模板 - templateId: {}, owner: {}", templateId, template.getCreatorId());
            return;
        }
        
        if (templateRepository != null) {
            templateRepository.deleteById(templateId);
        }
        
        log.info("删除模板 - templateId: {}", templateId);
    }
}
