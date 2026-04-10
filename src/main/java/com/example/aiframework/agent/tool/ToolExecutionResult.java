package com.example.aiframework.agent.tool;

/**
 * 工具执行结果
 */
public class ToolExecutionResult {
    
    private boolean success;
    private String output;
    private String error;
    
    public ToolExecutionResult() {}
    
    public ToolExecutionResult(boolean success, String output, String error) {
        this.success = success;
        this.output = output;
        this.error = error;
    }
    
    public static ToolExecutionResult success(String output) {
        return new ToolExecutionResult(true, output, null);
    }
    
    public static ToolExecutionResult error(String error) {
        return new ToolExecutionResult(false, null, error);
    }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
