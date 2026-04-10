package com.example.aiframework.knowledge.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 集合字段定义
 */
public class FieldSchema {
    
    /**
     * 字段名称
     */
    private String name;
    
    /**
     * 字段类型
     * VECTOR, VARCHAR, INT64, FLOAT, JSON, etc.
     */
    private String dataType;
    
    /**
     * 是否为自动 ID
     */
    private Boolean autoId;
    
    /**
     * 是否为向量字段
     */
    private Boolean isPrimary;
    
    /**
     * 字段描述
     */
    private String description;
    
    /**
     * 最大长度（VARCHAR 类型使用）
     */
    private Integer maxLength;
    
    /**
     * 向量维度（VECTOR 类型使用）
     */
    private Integer dimension;
    
    public FieldSchema() {}
    
    public FieldSchema(String name, String dataType) {
        this.name = name;
        this.dataType = dataType;
    }
    
    /**
     * 创建向量字段
     */
    public static FieldSchema vectorField(String name, int dimension) {
        FieldSchema field = new FieldSchema(name, "FLOAT_VECTOR");
        field.setDimension(dimension);
        return field;
    }
    
    /**
     * 创建主键字段
     */
    public static FieldSchema primaryKeyField(String name) {
        FieldSchema field = new FieldSchema(name, "VarChar");
        field.setIsPrimary(true);
        field.setAutoId(false);
        field.setMaxLength(64);
        return field;
    }
    
    /**
     * 创建文本字段
     */
    public static FieldSchema textField(String name, int maxLength) {
        FieldSchema field = new FieldSchema(name, "VarChar");
        field.setMaxLength(maxLength);
        return field;
    }
    
    // Getters and Setters
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDataType() {
        return dataType;
    }
    
    public void setDataType(String dataType) {
        this.dataType = dataType;
    }
    
    public Boolean getAutoId() {
        return autoId;
    }
    
    public void setAutoId(Boolean autoId) {
        this.autoId = autoId;
    }
    
    public Boolean getIsPrimary() {
        return isPrimary;
    }
    
    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getMaxLength() {
        return maxLength;
    }
    
    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }
    
    public Integer getDimension() {
        return dimension;
    }
    
    public void setDimension(Integer dimension) {
        this.dimension = dimension;
    }
}
