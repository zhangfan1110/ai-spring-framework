package com.example.aiframework.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 消息信息
 */
@Setter
@Getter
public class ChatSessionExportDTO_MessageInfo {
    private String id;
    private String role;
    private String content;
    private String model;
    private Integer tokens;
    private java.time.LocalDateTime createTime;

}
