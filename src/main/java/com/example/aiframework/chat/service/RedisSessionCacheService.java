package com.example.aiframework.chat.service;

import com.example.aiframework.chat.entity.ChatMessageEntity;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 会话缓存服务
 */
@Service
public class RedisSessionCacheService {
    
    private static final Logger log = LoggerFactory.getLogger(RedisSessionCacheService.class);
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Value("${redis.cache.prefix:chat:session:}")
    private String keyPrefix;
    
    @Value("${redis.cache.ttl-hours:24}")
    private int ttlHours;
    
    @Value("${redis.cache.max-size:100}")
    private int maxSize;
    
    /**
     * 获取缓存的会话历史
     */
    @SuppressWarnings("unchecked")
    public List<dev.langchain4j.data.message.ChatMessage> getSessionHistory(String sessionId) {
        String key = keyPrefix + sessionId;
        
        try {
            List<Object> messages = redisTemplate.opsForList().range(key, 0, -1);
            if (messages == null || messages.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<dev.langchain4j.data.message.ChatMessage> result = new ArrayList<>();
            for (Object msg : messages) {
                if (msg instanceof Map) {
                    Map<String, String> map = (Map<String, String>) msg;
                    String role = map.get("role");
                    String content = map.get("content");
                    
                    switch (role) {
                        case "SYSTEM":
                            result.add(SystemMessage.from(content));
                            break;
                        case "AI":
                            result.add(AiMessage.from(content));
                            break;
                        case "USER":
                        default:
                            result.add(UserMessage.from(content));
                            break;
                    }
                }
            }
            
            log.debug("从 Redis 加载会话历史：{} - {} 条消息", sessionId, result.size());
            return result;
            
        } catch (Exception e) {
            log.error("从 Redis 加载会话失败：{}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 保存会话历史到 Redis
     */
    public void saveSessionHistory(String sessionId, List<dev.langchain4j.data.message.ChatMessage> messages) {
        String key = keyPrefix + sessionId;
        
        try {
            // 先删除旧数据
            redisTemplate.delete(key);
            
            // 转换为 Map 存储
            List<Map<String, String>> messageMaps = new ArrayList<>();
            for (dev.langchain4j.data.message.ChatMessage msg : messages) {
                Map<String, String> map = Map.of(
                    "role", msg.type().name(),
                    "content", msg.text()
                );
                messageMaps.add(map);
            }
            
            // 批量保存
            for (Map<String, String> msgMap : messageMaps) {
                redisTemplate.opsForList().rightPush(key, msgMap);
            }
            
            // 限制大小
            if (messages.size() > maxSize) {
                redisTemplate.opsForList().trim(key, messages.size() - maxSize, -1);
            }
            
            // 设置过期时间
            redisTemplate.expire(key, ttlHours, TimeUnit.HOURS);
            
            log.debug("保存会话到 Redis：{} - {} 条消息，TTL: {}小时", sessionId, messages.size(), ttlHours);
            
        } catch (Exception e) {
            log.error("保存会话到 Redis 失败：{}", e.getMessage(), e);
        }
    }
    
    /**
     * 追加消息到会话
     */
    public void appendMessage(String sessionId, dev.langchain4j.data.message.ChatMessage message) {
        String key = keyPrefix + sessionId;
        
        try {
            Map<String, String> msgMap = Map.of(
                "role", message.type().name(),
                "content", message.text()
            );
            
            redisTemplate.opsForList().rightPush(key, msgMap);
            
            // 限制大小
            Long size = redisTemplate.opsForList().size(key);
            if (size != null && size > maxSize) {
                redisTemplate.opsForList().trim(key, size - maxSize, -1);
            }
            
            // 刷新过期时间
            redisTemplate.expire(key, ttlHours, TimeUnit.HOURS);
            
        } catch (Exception e) {
            log.error("追加消息到 Redis 失败：{}", e.getMessage(), e);
        }
    }
    
    /**
     * 删除会话缓存
     */
    public void deleteSession(String sessionId) {
        String key = keyPrefix + sessionId;
        
        try {
            redisTemplate.delete(key);
            log.debug("删除会话缓存：{}", sessionId);
        } catch (Exception e) {
            log.error("删除会话缓存失败：{}", e.getMessage(), e);
        }
    }
    
    /**
     * 检查会话是否存在
     */
    public boolean hasSession(String sessionId) {
        String key = keyPrefix + sessionId;
        
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("检查会话存在失败：{}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取所有会话 Key
     */
    public Set<String> getAllSessionKeys() {
        try {
            return redisTemplate.keys(keyPrefix + "*");
        } catch (Exception e) {
            log.error("获取所有会话 Key 失败：{}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 获取会话消息数量
     */
    public Long getSessionSize(String sessionId) {
        String key = keyPrefix + sessionId;
        
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            log.error("获取会话大小失败：{}", e.getMessage(), e);
            return 0L;
        }
    }
    
    /**
     * 刷新会话过期时间
     */
    public void refreshTTL(String sessionId) {
        String key = keyPrefix + sessionId;
        
        try {
            redisTemplate.expire(key, ttlHours, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("刷新 TTL 失败：{}", e.getMessage(), e);
        }
    }
    
    /**
     * 清空所有会话缓存
     */
    public void clearAll() {
        try {
            Set<String> keys = getAllSessionKeys();
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("清空所有会话缓存，共 {} 个", keys.size());
            }
        } catch (Exception e) {
            log.error("清空所有缓存失败：{}", e.getMessage(), e);
        }
    }
}
