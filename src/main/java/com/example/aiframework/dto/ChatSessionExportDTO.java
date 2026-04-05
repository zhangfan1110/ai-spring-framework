package com.example.aiframework.dto;

import java.util.List;

/**
 * 会话导出 DTO
 */
public class ChatSessionExportDTO {
    
    private ChatSessionExportDTO_ExportMetadata metadata;
    private ChatSessionExportDTO_SessionInfo session;
    private List<ChatSessionExportDTO_MessageInfo> messages;
    private ChatSessionExportDTO_SummaryInfo summary;
    
    public ChatSessionExportDTO() {}
    
    public ChatSessionExportDTO_ExportMetadata getMetadata() { return metadata; }
    public void setMetadata(ChatSessionExportDTO_ExportMetadata metadata) { this.metadata = metadata; }
    
    public ChatSessionExportDTO_SessionInfo getSession() { return session; }
    public void setSession(ChatSessionExportDTO_SessionInfo session) { this.session = session; }
    
    public List<ChatSessionExportDTO_MessageInfo> getMessages() { return messages; }
    public void setMessages(List<ChatSessionExportDTO_MessageInfo> messages) { this.messages = messages; }
    
    public ChatSessionExportDTO_SummaryInfo getSummary() { return summary; }
    public void setSummary(ChatSessionExportDTO_SummaryInfo summary) { this.summary = summary; }
}
