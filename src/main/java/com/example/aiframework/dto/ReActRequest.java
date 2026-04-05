package com.example.aiframework.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * ReAct 执行请求
 */
@Setter
@Getter
public class ReActRequest {
    
    private String task;
    private Integer maxIterations;
    
    public ReActRequest() {}

}
