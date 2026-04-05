package com.example.aiframework.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 会话导出 DTO - 手动实现 getter/setter (避免 Lombok 问题)
 */
@Setter
@Getter
public class ChatSessionExportDTO {
    
    private ChatSessionExportDTO_ExportMetadata metadata;
    private ChatSessionExportDTO_SessionInfo session;
    private List<ChatSessionExportDTO_MessageInfo> messages;
    private ChatSessionExportDTO_SummaryInfo summary;
    
    public ChatSessionExportDTO() {}

}
