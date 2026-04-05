package com.example.aiframework.tool;

/**
 * 工具参数
 */
public class ToolParameter {
    
    private String name;
    private String type;  // string, number, boolean, array
    private String description;
    private boolean required;
    private Object defaultValue;
    
    public ToolParameter() {}
    
    public ToolParameter(String name, String type, String description, boolean required) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.required = required;
    }
    
    public static ToolParameter string(String name, String description, boolean required) {
        return new ToolParameter(name, "string", description, required);
    }
    
    public static ToolParameter number(String name, String description, boolean required) {
        return new ToolParameter(name, "number", description, required);
    }
    
    public static ToolParameter bool(String name, String description, boolean required) {
        return new ToolParameter(name, "boolean", description, required);
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    
    public Object getDefaultValue() { return defaultValue; }
    public void setDefaultValue(Object defaultValue) { this.defaultValue = defaultValue; }
}
