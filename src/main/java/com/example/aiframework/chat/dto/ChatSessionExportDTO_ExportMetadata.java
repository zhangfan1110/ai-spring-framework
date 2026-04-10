package com.example.aiframework.chat.dto;

import java.time.LocalDateTime;

/**
 * 会话导出元数据
 */
public class ChatSessionExportDTO_ExportMetadata {
    private String version;
    private LocalDateTime exportTime;
    private String exportedBy;
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public LocalDateTime getExportTime() { return exportTime; }
    public void setExportTime(LocalDateTime exportTime) { this.exportTime = exportTime; }
    public String getExportedBy() { return exportedBy; }
    public void setExportedBy(String exportedBy) { this.exportedBy = exportedBy; }
}
