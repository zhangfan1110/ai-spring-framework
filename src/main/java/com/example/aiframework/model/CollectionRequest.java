package com.example.aiframework.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 集合创建请求
 */
public class CollectionRequest {
    
    private String collectionName;
    private String description;
    private Integer dimension;
    private String metricType;
    private String indexType;
    private List<FieldSchema> fields;
    
    public CollectionRequest() {}
    
    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Integer getDimension() { return dimension; }
    public void setDimension(Integer dimension) { this.dimension = dimension; }
    
    public String getMetricType() { return metricType; }
    public void setMetricType(String metricType) { this.metricType = metricType; }
    
    public String getIndexType() { return indexType; }
    public void setIndexType(String indexType) { this.indexType = indexType; }
    
    public List<FieldSchema> getFields() { return fields; }
    public void setFields(List<FieldSchema> fields) { this.fields = fields; }
    
    public List<FieldSchema> getFieldsOrDefault() {
        if (fields == null || fields.isEmpty()) {
            List<FieldSchema> defaultFields = new ArrayList<>();
            defaultFields.add(FieldSchema.primaryKeyField("id"));
            defaultFields.add(FieldSchema.vectorField("embedding", dimension != null ? dimension : 1024));
            defaultFields.add(FieldSchema.textField("content", 65535));
            defaultFields.add(FieldSchema.textField("metadata", 65535));
            return defaultFields;
        }
        return fields;
    }
}
