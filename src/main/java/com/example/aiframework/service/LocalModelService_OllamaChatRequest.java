package com.example.aiframework.service;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/**
 * Ollama 聊天请求
 */
@Setter
@Getter
public class LocalModelService_OllamaChatRequest {
    private String model;
    private List<LocalModelService_OllamaChatRequest_Message> messages;
    private Double temperature;
    private Boolean stream;
    
    public LocalModelService_OllamaChatRequest(String model, String prompt, double temperature, boolean stream) {
        this.model = model;
        this.messages = Collections.singletonList(new LocalModelService_OllamaChatRequest_Message("user", prompt));
        this.temperature = temperature;
        this.stream = stream;
    }

}
