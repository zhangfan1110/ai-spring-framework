package com.example.aiframework.service;

import com.example.aiframework.dto.ChatSessionExportDTO;
import com.example.aiframework.dto.ChatSessionExportDTO_ExportMetadata;
import com.example.aiframework.dto.ChatSessionExportDTO_SessionInfo;
import com.example.aiframework.dto.ChatSessionExportDTO_MessageInfo;
import com.example.aiframework.dto.ChatSessionExportDTO_SummaryInfo;
import com.example.aiframework.entity.ChatMessageEntity;
import com.example.aiframework.entity.ChatSessionEntity;
import com.example.aiframework.entity.ChatSessionSummaryEntity;
import com.example.aiframework.repository.ChatMessageRepository;
import com.example.aiframework.repository.ChatSessionRepository;
import com.example.aiframework.repository.ChatSessionSummaryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会话导出/导入服务
 */
@Service
public class ExportImportService {
    
    private static final Logger log = LoggerFactory.getLogger(ExportImportService.class);
    
    @Autowired
    private ChatSessionRepository sessionRepository;
    
    @Autowired
    private ChatMessageRepository messageRepository;
    
    @Autowired
    private ChatSessionSummaryRepository summaryRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static final String EXPORT_VERSION = "1.0";
    
    /**
     * 导出会话为 JSON
     */
    public ChatSessionExportDTO exportSession(String sessionId) {
        log.info("导出会话：{}", sessionId);
        
        // 查询会话信息
        ChatSessionEntity session = sessionRepository.findById(sessionId);
        if (session == null) {
            return null;
        }
        
        // 查询消息列表
        List<ChatMessageEntity> messages = messageRepository.findBySessionId(sessionId);
        
        // 查询会话摘要
        ChatSessionSummaryEntity summary = summaryRepository.findBySessionId(sessionId);
        
        // 构建导出 DTO
        ChatSessionExportDTO exportDTO = new ChatSessionExportDTO();
        
        // 元数据
        ChatSessionExportDTO_ExportMetadata metadata = new ChatSessionExportDTO_ExportMetadata();
        metadata.setVersion(EXPORT_VERSION);
        metadata.setExportTime(LocalDateTime.now());
        metadata.setExportedBy("system");
        exportDTO.setMetadata(metadata);
        
        // 会话信息
        ChatSessionExportDTO_SessionInfo sessionInfo = new ChatSessionExportDTO_SessionInfo();
        sessionInfo.setSessionId(session.getSessionId());
        sessionInfo.setTitle(session.getTitle());
        sessionInfo.setMessageCount(session.getMessageCount());
        sessionInfo.setCreateTime(session.getCreateTime());
        sessionInfo.setLastActiveTime(session.getLastActiveTime());
        exportDTO.setSession(sessionInfo);
        
        // 消息列表
        List<ChatSessionExportDTO_MessageInfo> messageInfos = messages.stream()
            .map(msg -> {
                ChatSessionExportDTO_MessageInfo info = new ChatSessionExportDTO_MessageInfo();
                info.setId(msg.getId());
                info.setRole(msg.getRole());
                info.setContent(msg.getContent());
                info.setModel(msg.getModel());
                info.setTokens(msg.getTokens());
                info.setCreateTime(msg.getCreateTime());
                return info;
            })
            .collect(Collectors.toList());
        exportDTO.setMessages(messageInfos);
        
        // 会话摘要
        if (summary != null) {
            ChatSessionExportDTO_SummaryInfo summaryInfo = new ChatSessionExportDTO_SummaryInfo();
            summaryInfo.setSummary(summary.getSummary());
            summaryInfo.setKeyPoints(summary.getKeyPoints());
            summaryInfo.setCreateTime(summary.getCreateTime());
            exportDTO.setSummary(summaryInfo);
        }
        
        log.info("导出会话完成：{} 条消息", messages.size());
        return exportDTO;
    }
    
    /**
     * 导出会话为 JSON 字符串
     */
    public String exportSessionToJson(String sessionId) throws IOException {
        ChatSessionExportDTO exportDTO = exportSession(sessionId);
        if (exportDTO == null) {
            return null;
        }
        return objectMapper.writeValueAsString(exportDTO);
    }
    
    /**
     * 从 JSON 导入会话
     */
    public ChatSessionEntity importSession(ChatSessionExportDTO exportDTO) {
        log.info("导入会话：{}", exportDTO.getSession().getSessionId());
        
        try {
            // 恢复会话
            ChatSessionEntity session = new ChatSessionEntity();
            session.setSessionId(exportDTO.getSession().getSessionId());
            session.setTitle(exportDTO.getSession().getTitle());
            session.setMessageCount(exportDTO.getSession().getMessageCount());
            session.setCreateTime(exportDTO.getSession().getCreateTime());
            session.setLastActiveTime(exportDTO.getSession().getLastActiveTime());
            sessionRepository.save(session);
            
            // 恢复消息
            if (exportDTO.getMessages() != null) {
                for (ChatSessionExportDTO_MessageInfo msgInfo : exportDTO.getMessages()) {
                    ChatMessageEntity message = new ChatMessageEntity();
                    message.setId(msgInfo.getId());
                    message.setSessionId(session.getSessionId());
                    message.setRole(msgInfo.getRole());
                    message.setContent(msgInfo.getContent());
                    message.setModel(msgInfo.getModel());
                    message.setTokens(msgInfo.getTokens());
                    message.setCreateTime(msgInfo.getCreateTime());
                    messageRepository.save(message);
                }
            }
            
            // 恢复摘要
            if (exportDTO.getSummary() != null) {
                ChatSessionSummaryEntity summary = new ChatSessionSummaryEntity();
                summary.setSessionId(session.getSessionId());
                summary.setSummary(exportDTO.getSummary().getSummary());
                summary.setKeyPoints(exportDTO.getSummary().getKeyPoints());
                summary.setCreateTime(exportDTO.getSummary().getCreateTime());
                summaryRepository.save(summary);
            }
            
            log.info("导入会话完成：{} 条消息", 
                exportDTO.getMessages() != null ? exportDTO.getMessages().size() : 0);
            
            return session;
            
        } catch (Exception e) {
            log.error("导入会话失败：{}", e.getMessage(), e);
            throw new RuntimeException("导入会话失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 从 JSON 字符串导入会话
     */
    public ChatSessionEntity importSessionFromJson(String json) throws IOException {
        ChatSessionExportDTO exportDTO = objectMapper.readValue(json, ChatSessionExportDTO.class);
        return importSession(exportDTO);
    }
    
    /**
     * 从文件导入会话
     */
    public ChatSessionEntity importSessionFromFile(MultipartFile file) throws IOException {
        String json = new String(file.getBytes(), StandardCharsets.UTF_8);
        return importSessionFromJson(json);
    }
}
