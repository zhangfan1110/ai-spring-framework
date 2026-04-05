package com.example.aiframework.service;

import com.example.aiframework.entity.ChatSessionShareEntity;
import com.example.aiframework.repository.ChatSessionShareRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 会话分享服务
 */
@Service
public class SessionShareService {
    
    private static final Logger log = LoggerFactory.getLogger(SessionShareService.class);
    
    @Autowired(required = false)
    private ChatSessionShareRepository shareRepository;
    
    /**
     * 创建分享链接
     */
    public ChatSessionShareEntity createShare(String sessionId, String creatorId, ShareConfig config) {
        log.info("创建会话分享 - sessionId: {}, creator: {}", sessionId, creatorId);
        
        ChatSessionShareEntity share = new ChatSessionShareEntity();
        share.setShareId(UUID.randomUUID().toString());
        share.setSessionId(sessionId);
        share.setShareToken(generateShareToken());
        share.setShareTitle(config.shareTitle);
        share.setShareDescription(config.shareDescription);
        share.setIsPublic(config.isPublic);
        share.setAccessPassword(config.accessPassword);
        share.setExpireTime(config.expireDays != null ? LocalDateTime.now().plusDays(config.expireDays) : null);
        share.setMaxViews(config.maxViews);
        share.setCreatorId(creatorId);
        share.setCreateTime(LocalDateTime.now());
        share.setDisabled(false);
        
        if (shareRepository != null) {
            shareRepository.save(share);
        }
        
        log.info("分享链接创建成功 - shareToken: {}", share.getShareToken());
        return share;
    }
    
    /**
     * 根据令牌获取分享
     */
    public ChatSessionShareEntity getShareByToken(String shareToken) {
        if (shareRepository == null) {
            return null;
        }
        
        ChatSessionShareEntity share = shareRepository.findByShareToken(shareToken);
        
        if (share != null) {
            // 检查是否可用
            if (!share.isAvailable()) {
                log.warn("分享链接不可用 - shareToken: {}, expired: {}, maxViews: {}, disabled: {}",
                    shareToken, share.isExpired(), share.isMaxViewsReached(), share.getDisabled());
                return null;
            }
            
            // 增加访问次数
            share.incrementViewCount();
            shareRepository.incrementViewCount(share.getShareId());
        }
        
        return share;
    }
    
    /**
     * 验证访问密码
     */
    public boolean verifyPassword(ChatSessionShareEntity share, String password) {
        if (share == null || share.getAccessPassword() == null) {
            return true; // 无密码要求
        }
        
        return share.getAccessPassword().equals(password);
    }
    
    /**
     * 获取会话的所有分享
     */
    public List<ChatSessionShareEntity> getSessionShares(String sessionId) {
        return shareRepository != null ? shareRepository.findBySessionId(sessionId) : List.of();
    }
    
    /**
     * 获取用户的分享列表
     */
    public List<ChatSessionShareEntity> getUserShares(String creatorId) {
        return shareRepository != null ? shareRepository.findByCreatorId(creatorId) : List.of();
    }
    
    /**
     * 获取公开分享广场
     */
    public List<ChatSessionShareEntity> getPublicShares(int limit, int offset) {
        return shareRepository != null ? shareRepository.findPublicShares(limit, offset) : List.of();
    }
    
    /**
     * 禁用分享
     */
    public void disableShare(String shareId) {
        if (shareRepository != null) {
            shareRepository.setDisabled(shareId, true);
            log.info("禁用分享 - shareId: {}", shareId);
        }
    }
    
    /**
     * 删除分享
     */
    public void deleteShare(String shareId) {
        if (shareRepository != null) {
            shareRepository.deleteById(shareId);
            log.info("删除分享 - shareId: {}", shareId);
        }
    }
    
    /**
     * 生成分享令牌
     */
    private String generateShareToken() {
        // 生成 12 位随机令牌
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
    
    /**
     * 分享配置
     */
    public static class ShareConfig {
        public String shareTitle;
        public String shareDescription;
        public Boolean isPublic = false;
        public String accessPassword;
        public Integer expireDays; // 过期天数
        public Integer maxViews;   // 最大访问次数
        
        public ShareConfig title(String title) {
            this.shareTitle = title;
            return this;
        }
        
        public ShareConfig description(String desc) {
            this.shareDescription = desc;
            return this;
        }
        
        public ShareConfig publicShare(boolean isPublic) {
            this.isPublic = isPublic;
            return this;
        }
        
        public ShareConfig password(String password) {
            this.accessPassword = password;
            return this;
        }
        
        public ShareConfig expireDays(int days) {
            this.expireDays = days;
            return this;
        }
        
        public ShareConfig maxViews(int max) {
            this.maxViews = max;
            return this;
        }
    }
}
