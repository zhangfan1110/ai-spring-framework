package com.example.aiframework.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 调用链验证结果
 */
@Setter
@Getter
public class ChainValidationService_ValidationResult {
    private boolean valid;
    private List<String> errors;
    private List<String> warnings;
    private int nodeCount;
    private int estimatedDuration;
    private Map<String, Object> details;
    
    public ChainValidationService_ValidationResult() {
        this.valid = true;
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.details = new ConcurrentHashMap<>();
    }

    public void addError(String error) {
        this.errors.add(error);
        this.valid = false;
    }
    
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
}
