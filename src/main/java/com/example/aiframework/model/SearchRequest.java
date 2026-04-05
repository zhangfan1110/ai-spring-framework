package com.example.aiframework.model;

public class SearchRequest {
    
    private String collectionName;
    private String queryText;
    private Integer topK;
    private String filter;
    
    public SearchRequest() {}
    
    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    
    public String getQueryText() { return queryText; }
    public void setQueryText(String queryText) { this.queryText = queryText; }
    
    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }
    
    public String getFilter() { return filter; }
    public void setFilter(String filter) { this.filter = filter; }
}
