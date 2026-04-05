package com.example.aiframework.service;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 本地模型响应
 */
@Setter
@Getter
public class LocalModelService_ModelResponse {
    private String content;
    private String model;
    private long duration;
    private Map<String, Object> metadata;

}
