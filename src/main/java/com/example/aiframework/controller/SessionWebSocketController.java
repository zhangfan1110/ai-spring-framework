package com.example.aiframework.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话 WebSocket 控制器 - 实时协作
 */
@Controller
public class SessionWebSocketController {
    
    private static final Logger log = LoggerFactory.getLogger(SessionWebSocketController.class);
    
    private final SimpMessageSendingOperations messagingTemplate;
    
    // 在线用户追踪 (sessionId -> 用户数)
    private final Map<String, Integer> onlineUsers = new ConcurrentHashMap<>();
    
    // 用户最后活跃时间
    private final Map<String, LocalDateTime> userActivity = new ConcurrentHashMap<>();
    
    public SessionWebSocketController(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    
    /**
     * 用户加入会话
     */
    @MessageMapping("/session.{sessionId}.join")
    @SendTo("/topic/session.{sessionId}")
    public Map<String, Object> joinSession(@DestinationVariable String sessionId, 
                                           Map<String, String> payload) {
        String userId = payload.get("userId");
        String userName = payload.get("userName");
        
        log.info("用户加入会话 - sessionId: {}, userId: {}, userName: {}", sessionId, userId, userName);
        
        // 更新在线用户数
        onlineUsers.merge(sessionId, 1, Integer::sum);
        userActivity.put(userId, LocalDateTime.now());
        
        return buildSystemMessage("join", userName + " 加入了会话", sessionId);
    }
    
    /**
     * 用户离开会话
     */
    @MessageMapping("/session.{sessionId}.leave")
    @SendTo("/topic/session.{sessionId}")
    public Map<String, Object> leaveSession(@DestinationVariable String sessionId,
                                            Map<String, String> payload) {
        String userId = payload.get("userId");
        String userName = payload.get("userName");
        
        log.info("用户离开会话 - sessionId: {}, userId: {}", sessionId, userId);
        
        // 更新在线用户数
        onlineUsers.computeIfPresent(sessionId, (k, v) -> Math.max(0, v - 1));
        
        return buildSystemMessage("leave", userName + " 离开了会话", sessionId);
    }
    
    /**
     * 发送消息 (协作聊天)
     */
    @MessageMapping("/session.{sessionId}.message")
    @SendTo("/topic/session.{sessionId}")
    public Map<String, Object> sendMessage(@DestinationVariable String sessionId,
                                           Map<String, Object> payload) {
        String userId = payload.get("userId").toString();
        String userName = payload.get("userName").toString();
        String content = payload.get("content").toString();
        
        log.info("会话消息 - sessionId: {}, userId: {}, content: {}", sessionId, userId, content);
        
        // 更新活跃时间
        userActivity.put(userId, LocalDateTime.now());
        
        return Map.of(
            "type", "message",
            "sessionId", sessionId,
            "userId", userId,
            "userName", userName,
            "content", content,
            "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }
    
    /**
     * 输入中状态
     */
    @MessageMapping("/session.{sessionId}.typing")
    @SendTo("/topic/session.{sessionId}.typing")
    public Map<String, Object> typing(@DestinationVariable String sessionId,
                                      Map<String, String> payload) {
        String userId = payload.get("userId");
        String userName = payload.get("userName");
        
        return Map.of(
            "type", "typing",
            "userId", userId,
            "userName", userName,
            "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }
    
    /**
     * AI 响应流式推送
     */
    @MessageMapping("/session.{sessionId}.ai-response")
    @SendTo("/topic/session.{sessionId}")
    public Map<String, Object> aiResponse(@DestinationVariable String sessionId,
                                          Map<String, Object> payload) {
        String content = payload.get("content").toString();
        String messageId = payload.get("messageId").toString();
        Boolean isComplete = (Boolean) payload.get("isComplete");
        
        return Map.of(
            "type", "ai_response",
            "sessionId", sessionId,
            "messageId", messageId,
            "content", content,
            "isComplete", isComplete != null ? isComplete : false,
            "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }
    
    /**
     * 获取在线用户数
     */
    @SubscribeMapping("/session.{sessionId}.online")
    public Map<String, Object> getOnlineUsers(@DestinationVariable String sessionId) {
        int count = onlineUsers.getOrDefault(sessionId, 0);
        log.info("查询在线用户 - sessionId: {}, count: {}", sessionId, count);
        
        return Map.of(
            "type", "online_count",
            "sessionId", sessionId,
            "count", count
        );
    }
    
    /**
     * 构建系统消息
     */
    private Map<String, Object> buildSystemMessage(String eventType, String text, String sessionId) {
        return Map.of(
            "type", "system",
            "event", eventType,
            "text", text,
            "sessionId", sessionId,
            "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }
}
