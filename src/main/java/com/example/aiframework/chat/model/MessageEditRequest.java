package com.example.aiframework.chat.model;

/**
 * 消息编辑请求
 */
public class MessageEditRequest {
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 元数据
     */
    private Object metadata;
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public Object getMetadata() { return metadata; }
    public void setMetadata(Object metadata) { this.metadata = metadata; }
}
