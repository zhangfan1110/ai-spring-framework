package com.example.aiframework.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 会话信息
 */
@Setter
@Getter
public class ChatSessionExportDTO_SessionInfo {
    private String sessionId;
    private String title;
    private Integer messageCount;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime lastActiveTime;

}
