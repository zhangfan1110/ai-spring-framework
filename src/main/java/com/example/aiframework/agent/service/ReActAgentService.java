package com.example.aiframework.agent.service;

import com.example.aiframework.agent.tool.ToolExecutionResult;
import com.example.aiframework.agent.tool.ToolManager;
import com.example.aiframework.agent.tool.ToolParameter;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 增强版 ReAct 智能体服务
 * 
 * 新增功能：
 * 1. 工具调用链 - 支持多个工具顺序/并行执行
 * 2. 多 Agent 协作 - 支持多个 Agent 角色协同工作
 * 3. 执行计划 - 预先规划工具执行步骤
 * 4. 错误恢复 - 工具失败时自动重试或切换策略
 */
@Service
public class ReActAgentService {
    
    private static final Logger log = LoggerFactory.getLogger(ReActAgentService.class);
    
    @Resource
    private ChatLanguageModel chatModel;
    
    @Autowired
    private ToolManager toolManager;
    
    @Autowired(required = false)
    private ReActContextService contextService;
    
    @Value("${agent.react.max-iterations:10}")
    private int maxIterations;
    
    @Value("${agent.react.max-retries:2}")
    private int maxRetries;
    
    // Agent 角色定义
    public enum AgentRole {
        RESEARCHER("研究员", "负责信息搜集和调研"),
        ANALYST("分析师", "负责数据分析和推理"),
        CODER("程序员", "负责代码编写和执行"),
        WRITER("作家", "负责内容创作和撰写"),
        REVIEWER("审核员", "负责质量审核和验证"),
        PLANNER("规划师", "负责任务分解和规划");
        
        private final String displayName;
        private final String description;
        
        AgentRole(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    /**
     * 工具调用链节点
     */
    public static class ChainNode {
        private String stepId;
        private String toolName;
        private Map<String, String> parameters;
        private String dependsOn; // 依赖的前置步骤 ID
        private boolean parallel = false; // 是否可并行执行
        
        public String getStepId() { return stepId; }
        public void setStepId(String stepId) { this.stepId = stepId; }
        
        public String getToolName() { return toolName; }
        public void setToolName(String toolName) { this.toolName = toolName; }
        
        public Map<String, String> getParameters() { return parameters; }
        public void setParameters(Map<String, String> parameters) { this.parameters = parameters; }
        
        public String getDependsOn() { return dependsOn; }
        public void setDependsOn(String dependsOn) { this.dependsOn = dependsOn; }
        
        public boolean isParallel() { return parallel; }
        public void setParallel(boolean parallel) { this.parallel = parallel; }
    }
    
    /**
     * 工具调用链
     */
    public static class ToolChain {
        private String chainId;
        private String description;
        private List<ChainNode> nodes;
        private Map<String, Object> context; // 执行上下文
        
        public ToolChain() {
            this.chainId = UUID.randomUUID().toString();
            this.nodes = new ArrayList<>();
            this.context = new ConcurrentHashMap<>();
        }
        
        public String getChainId() { return chainId; }
        public void setChainId(String chainId) { this.chainId = chainId; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public List<ChainNode> getNodes() { return nodes; }
        public void setNodes(List<ChainNode> nodes) { this.nodes = nodes; }
        
        public Map<String, Object> getContext() { return context; }
        public void setContext(Map<String, Object> context) { this.context = context; }
        
        public void addNode(ChainNode node) {
            this.nodes.add(node);
        }
    }
    
    /**
     * 多 Agent 协作会话
     */
    public static class CollaborationSession {
        private String sessionId;
        private String task;
        private Map<String, AgentRole> agents; // agentId -> role
        private List<String> messageHistory;
        private Map<String, Object> sharedContext;
        private String currentSpeaker;
        private boolean completed;
        
        public CollaborationSession() {
            this.sessionId = UUID.randomUUID().toString();
            this.agents = new LinkedHashMap<>();
            this.messageHistory = new ArrayList<>();
            this.sharedContext = new ConcurrentHashMap<>();
            this.completed = false;
        }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getTask() { return task; }
        public void setTask(String task) { this.task = task; }
        
        public Map<String, AgentRole> getAgents() { return agents; }
        public void setAgents(Map<String, AgentRole> agents) { this.agents = agents; }
        
        public List<String> getMessageHistory() { return messageHistory; }
        public void setMessageHistory(List<String> messageHistory) { this.messageHistory = messageHistory; }
        
        public Map<String, Object> getSharedContext() { return sharedContext; }
        public void setSharedContext(Map<String, Object> sharedContext) { this.sharedContext = sharedContext; }
        
        public String getCurrentSpeaker() { return currentSpeaker; }
        public void setCurrentSpeaker(String currentSpeaker) { this.currentSpeaker = currentSpeaker; }
        
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        
        public void addMessage(String agentId, String message) {
            this.messageHistory.add("[" + agentId + "]: " + message);
        }
    }
    
    // ========== 基础 ReAct 执行 ==========
    
    /**
     * ReAct 推理执行（基础版）
     */
    public String execute(String task) {
        return executeWithRole(task, null);
    }
    
    /**
     * ReAct 推理执行（带角色）
     */
    public String executeWithRole(String task, AgentRole role) {
        return executeWithContext(task, role, null);
    }
    
    /**
     * ReAct 推理执行（带上下文）
     * @param task 任务描述
     * @param role Agent 角色
     * @param contextId 上下文 ID（可选）
     * @return 执行结果
     */
    public String executeWithContext(String task, AgentRole role, String contextId) {
        log.info("ReAct 执行任务：{}, 角色：{}, 上下文：{}", task, 
            role != null ? role.getDisplayName() : "通用", contextId);
        
        // 构建带上下文的 Prompt
        String prompt;
        if (contextService != null && contextId != null) {
            String roleDesc = role != null ? role.getDisplayName() : "智能助手";
            prompt = contextService.buildContextualPrompt(contextId, task, roleDesc);
            
            // 记录用户输入到对话历史
            contextService.addConversationMessage(contextId, "USER", task);
        } else {
            prompt = buildSystemPrompt(role) + "\n\n任务：" + task + "\n\n开始执行:\n";
        }
        
        StringBuilder messages = new StringBuilder(prompt);
        
        List<String> thoughts = new ArrayList<>();
        int retryCount = 0;
        
        for (int i = 0; i < maxIterations; i++) {
            log.info("ReAct 迭代 {}/{}, 重试：{}", i + 1, maxIterations, retryCount);
            
            String response = chatModel.generate(messages.toString());
            log.debug("LLM 响应：{}", response);
            
            String thought = extractThought(response);
            if (thought != null) {
                thoughts.add(thought);
                log.info("思考：{}", thought);
            }
            
            String[] action = extractAction(response);
            if (action != null) {
                String toolName = action[0];
                String toolInput = action[1];
                
                log.info("行动：调用工具 {}，输入：{}", toolName, toolInput);
                
                // 执行工具（带重试）
                ToolExecutionResult result = executeWithRetry(toolName, toolInput, retryCount);
                
                if (!result.isSuccess() && retryCount < maxRetries) {
                    retryCount++;
                    log.warn("工具执行失败，重试 {}/{}", retryCount, maxRetries);
                    messages.append("\nObservation: 工具执行失败：" + result.getError() + "，请重试或尝试其他方法\n");
                    continue;
                }
                
                retryCount = 0;
                String observation = result.isSuccess() ? result.getOutput() : "错误：" + result.getError();
                log.info("观察：{}", observation);
                
                messages.append("\nThought: ").append(thought != null ? thought : "分析中...");
                messages.append("\nAction: ").append(toolName);
                messages.append("\nAction Input: ").append(toolInput);
                messages.append("\nObservation: ").append(observation);
                
            } else {
                String finalAnswer = extractFinalAnswer(response);
                if (finalAnswer != null) {
                    log.info("最终答案：{}", finalAnswer);
                    
                    // 记录 AI 回答到对话历史
                    if (contextService != null && contextId != null) {
                        contextService.addConversationMessage(contextId, "AI", finalAnswer);
                    }
                    
                    return finalAnswer;
                }
                
                messages.append("\n").append(response);
            }
        }
        
        log.warn("达到最大迭代次数");
        String conclusion = "经过分析，" + (thoughts.isEmpty() ? "我无法完成该任务。" : thoughts.get(thoughts.size() - 1));
        
        // 记录 AI 回答到对话历史
        if (contextService != null && contextId != null) {
            contextService.addConversationMessage(contextId, "AI", conclusion);
        }
        
        return conclusion;
    }
    
    /**
     * 带重试的工具执行
     */
    private ToolExecutionResult executeWithRetry(String toolName, String toolInput, int retryCount) {
        List<ToolParameter> parameters = parseToolInput(toolInput);
        ToolExecutionResult result = toolManager.executeTool(toolName, parameters);
        
        // 如果失败且还有重试次数，尝试简化参数后重试
        while (!result.isSuccess() && retryCount < maxRetries) {
            retryCount++;
            log.warn("工具 {} 执行失败，尝试简化参数后重试 {}/{}", toolName, retryCount, maxRetries);
            
            // 简化参数：只保留 input 字段
            List<ToolParameter> simpleParams = new ArrayList<>();
            ToolParameter simpleParam = new ToolParameter();
            simpleParam.setName("input");
            simpleParam.setDefaultValue(toolInput);
            simpleParams.add(simpleParam);
            
            result = toolManager.executeTool(toolName, simpleParams);
        }
        
        return result;
    }
    
    // ========== 工具调用链 ==========
    
    /**
     * 执行工具调用链
     */
    public Map<String, Object> executeChain(ToolChain chain) {
        log.info("执行工具调用链：{}", chain.getDescription());
        
        Map<String, Object> results = new HashMap<>();
        Map<String, ToolExecutionResult> nodeResults = new HashMap<>();
        
        // 按依赖关系排序执行
        List<ChainNode> orderedNodes = topologicalSort(chain.getNodes());
        
        for (ChainNode node : orderedNodes) {
            log.info("执行步骤：{} -> {}", node.getStepId(), node.getToolName());
            
            try {
                // 解析参数（支持引用前置步骤结果）
                Map<String, String> resolvedParams = resolveParameters(node.getParameters(), nodeResults);
                
                // 转换为 ToolParameter
                List<ToolParameter> params = new ArrayList<>();
                for (Map.Entry<String, String> entry : resolvedParams.entrySet()) {
                    ToolParameter param = new ToolParameter();
                    param.setName(entry.getKey());
                    param.setDefaultValue(entry.getValue());
                    params.add(param);
                }
                
                // 执行工具
                ToolExecutionResult result = toolManager.executeTool(node.getToolName(), params);
                nodeResults.put(node.getStepId(), result);
                
                // 保存到上下文
                chain.getContext().put(node.getStepId() + "_result", result.getOutput());
                
                if (!result.isSuccess()) {
                    log.error("步骤 {} 执行失败：{}", node.getStepId(), result.getError());
                    results.put("error", "步骤 " + node.getStepId() + " 失败：" + result.getError());
                    results.put("success", false);
                    return results;
                }
                
                results.put(node.getStepId(), result.getOutput());
                
            } catch (Exception e) {
                log.error("步骤 {} 执行异常：{}", node.getStepId(), e.getMessage(), e);
                results.put("error", "步骤 " + node.getStepId() + " 异常：" + e.getMessage());
                results.put("success", false);
                return results;
            }
        }
        
        results.put("success", true);
        results.put("chainId", chain.getChainId());
        results.put("context", chain.getContext());
        
        log.info("工具调用链执行完成");
        return results;
    }
    
    /**
     * 拓扑排序（处理依赖关系）
     */
    private List<ChainNode> topologicalSort(List<ChainNode> nodes) {
        // 简单实现：按依赖关系排序
        List<ChainNode> sorted = new ArrayList<>();
        Set<String> processed = new HashSet<>();
        
        for (ChainNode node : nodes) {
            if (node.getDependsOn() == null) {
                sorted.add(node);
                processed.add(node.getStepId());
            }
        }
        
        for (ChainNode node : nodes) {
            if (node.getDependsOn() != null && processed.contains(node.getDependsOn())) {
                sorted.add(node);
                processed.add(node.getStepId());
            }
        }
        
        // 处理剩余节点
        for (ChainNode node : nodes) {
            if (!processed.contains(node.getStepId())) {
                sorted.add(node);
            }
        }
        
        return sorted;
    }
    
    /**
     * 解析参数（支持引用前置步骤结果）
     */
    private Map<String, String> resolveParameters(Map<String, String> params, Map<String, ToolExecutionResult> results) {
        Map<String, String> resolved = new HashMap<>();
        
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String value = entry.getValue();
            
            // 替换 ${stepId_result} 为实际结果
            Pattern pattern = Pattern.compile("\\$\\{([^}]+)_result\\}");
            Matcher matcher = pattern.matcher(value);
            
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String stepId = matcher.group(1);
                ToolExecutionResult result = results.get(stepId);
                if (result != null && result.isSuccess()) {
                    matcher.appendReplacement(sb, result.getOutput());
                } else {
                    matcher.appendReplacement(sb, "");
                }
            }
            matcher.appendTail(sb);
            
            resolved.put(entry.getKey(), sb.toString());
        }
        
        return resolved;
    }
    
    // ========== 多 Agent 协作 ==========
    
    /**
     * 创建协作会话
     */
    public CollaborationSession createCollaborationSession(String task, List<AgentRole> roles) {
        log.info("创建协作会话：任务={}, 角色数={}", task, roles.size());
        
        CollaborationSession session = new CollaborationSession();
        session.setTask(task);
        
        for (int i = 0; i < roles.size(); i++) {
            String agentId = "agent_" + i;
            session.getAgents().put(agentId, roles.get(i));
        }
        
        return session;
    }
    
    /**
     * 执行协作会话
     */
    public String executeCollaboration(CollaborationSession session) {
        log.info("执行协作会话：{}", session.getSessionId());
        
        StringBuilder context = new StringBuilder();
        context.append("协作任务：").append(session.getTask()).append("\n\n");
        context.append("参与 Agent:\n");
        
        for (Map.Entry<String, AgentRole> entry : session.getAgents().entrySet()) {
            context.append("- ")
                   .append(entry.getKey())
                   .append(" (")
                   .append(entry.getValue().getDisplayName())
                   .append("): ")
                   .append(entry.getValue().getDescription())
                   .append("\n");
        }
        
        context.append("\n开始协作讨论:\n");
        session.addMessage("SYSTEM", context.toString());
        
        int maxRounds = session.getAgents().size() * 3; // 每个 Agent 最多发言 3 轮
        int round = 0;
        
        List<String> agentIds = new ArrayList<>(session.getAgents().keySet());
        int currentAgentIndex = 0;
        
        while (round < maxRounds && !session.isCompleted()) {
            String currentAgentId = agentIds.get(currentAgentIndex);
            AgentRole currentRole = session.getAgents().get(currentAgentId);
            
            session.setCurrentSpeaker(currentAgentId);
            
            // 构建当前 Agent 的 Prompt
            String agentPrompt = buildAgentPrompt(currentRole, session);
            String response = chatModel.generate(agentPrompt);
            
            session.addMessage(currentAgentId, response);
            log.info("[{}] 发言：{}", currentRole.getDisplayName(), response.substring(0, Math.min(100, response.length())));
            
            // 检查是否完成
            if (response.contains("任务完成") || response.contains("得出结论") || response.contains("Final Answer")) {
                session.setCompleted(true);
                break;
            }
            
            // 切换到下一个 Agent
            currentAgentIndex = (currentAgentIndex + 1) % agentIds.size();
            round++;
        }
        
        // 生成最终结论
        String finalConclusion = generateFinalConclusion(session);
        session.addMessage("SYSTEM", "最终结论：" + finalConclusion);
        
        log.info("协作会话完成");
        return finalConclusion;
    }
    
    /**
     * 构建 Agent Prompt
     */
    private String buildAgentPrompt(AgentRole role, CollaborationSession session) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("你是 ").append(role.getDisplayName()).append("，").append(role.getDescription()).append("\n\n");
        
        sb.append("当前任务：").append(session.getTask()).append("\n\n");
        
        sb.append("协作历史:\n");
        for (String msg : session.getMessageHistory()) {
            sb.append(msg).append("\n");
        }
        
        sb.append("\n请发表你的观点或执行你的任务。如果任务已完成，请说'任务完成'并给出结论。\n");
        
        return sb.toString();
    }
    
    /**
     * 生成最终结论
     */
    private String generateFinalConclusion(CollaborationSession session) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("协作总结:\n\n");
        sb.append("任务：").append(session.getTask()).append("\n\n");
        
        sb.append("讨论过程:\n");
        for (String msg : session.getMessageHistory()) {
            sb.append(msg).append("\n");
        }
        
        sb.append("\n请基于以上讨论，生成最终结论:");
        
        return chatModel.generate(sb.toString());
    }
    
    // ========== 辅助方法 ==========
    
    private String buildSystemPrompt(AgentRole role) {
        StringBuilder sb = new StringBuilder();
        
        if (role != null) {
            sb.append("你是 ").append(role.getDisplayName()).append("，").append(role.getDescription()).append("\n\n");
        } else {
            sb.append("你是一个智能助手，使用 ReAct 模式解决问题。\n\n");
        }
        
        sb.append("你可以使用以下工具:\n\n");
        sb.append(toolManager.generateToolsDescription());
        sb.append("\n请按照以下格式思考:\n\n");
        sb.append("Thought: 分析当前情况，决定下一步\n");
        sb.append("Action: 工具名称\n");
        sb.append("Action Input: 工具参数\n");
        sb.append("Observation: 工具执行结果\n");
        sb.append("\n重复以上步骤直到得出结论，然后:\n\n");
        sb.append("Final Answer: 你的最终答案\n\n");
        sb.append("记住:\n");
        sb.append("1. 每次只执行一个行动\n");
        sb.append("2. 根据观察结果决定下一步\n");
        sb.append("3. 得出结论时使用 Final Answer\n");
        sb.append("4. 如果工具执行失败，可以重试或尝试其他方法\n");
        
        return sb.toString();
    }
    
    private String extractThought(String text) {
        Matcher matcher = THOUGHT_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
    
    private String[] extractAction(String text) {
        Matcher matcher = ACTION_PATTERN.matcher(text);
        if (matcher.find()) {
            return new String[]{matcher.group(1).trim(), matcher.group(2).trim()};
        }
        return null;
    }
    
    private String extractFinalAnswer(String text) {
        Matcher matcher = FINAL_ANSWER_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
    
    private List<ToolParameter> parseToolInput(String input) {
        List<ToolParameter> parameters = new ArrayList<>();
        input = input.trim();
        
        if (input.startsWith("{") && input.endsWith("}")) {
            String content = input.substring(1, input.length() - 1);
            String[] pairs = content.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split(":");
                if (kv.length == 2) {
                    String key = kv[0].trim().replace("\"", "");
                    String value = kv[1].trim().replace("\"", "");
                    ToolParameter param = new ToolParameter();
                    param.setName(key);
                    param.setDefaultValue(value);
                    parameters.add(param);
                }
            }
        } else {
            ToolParameter param = new ToolParameter();
            param.setName("input");
            param.setDefaultValue(input);
            parameters.add(param);
        }
        
        return parameters;
    }
    
    private static final Pattern THOUGHT_PATTERN = Pattern.compile("Thought:\\s*(.+?)(?=\\n|Action:|$)", Pattern.DOTALL);
    private static final Pattern ACTION_PATTERN = Pattern.compile("Action:\\s*([\\w]+)\\s*\\n?Action Input:\\s*(.+?)(?=\\n|Thought:|Observation:|$)", Pattern.DOTALL);
    private static final Pattern FINAL_ANSWER_PATTERN = Pattern.compile("Final Answer:\\s*(.+)", Pattern.DOTALL);
    
    // ========== 流式输出支持 ==========
    
    /**
     * 流式事件类型
     */
    public enum StreamEventType {
        START("start"),
        THOUGHT("thought"),
        ACTION("action"),
        OBSERVATION("observation"),
        FINAL_ANSWER("final_answer"),
        ERROR("error"),
        END("end");
        
        private final String type;
        
        StreamEventType(String type) {
            this.type = type;
        }
        
        public String getType() { return type; }
    }
    
    /**
     * 流式事件
     */
    public static class StreamEvent {
        private String type;
        private String content;
        private long timestamp;
        private Map<String, Object> data;
        
        public StreamEvent(String type, String content) {
            this.type = type;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
            this.data = new HashMap<>();
        }
        
        public StreamEvent(String type, String content, Map<String, Object> data) {
            this.type = type;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
            this.data = data;
        }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }
    
    /**
     * 流式执行 ReAct 任务
     * @param task 任务描述
     * @return Flux 流式事件
     */
    public Flux<StreamEvent> executeStream(String task) {
        return executeStreamWithRole(task, null);
    }
    
    /**
     * 流式执行 ReAct 任务（带角色）
     * @param task 任务描述
     * @param role Agent 角色
     * @return Flux 流式事件
     */
    public Flux<StreamEvent> executeStreamWithRole(String task, AgentRole role) {
        log.info("ReAct 流式执行任务：{}, 角色：{}", task, role != null ? role.getDisplayName() : "通用");
        
        // 创建 Sink 用于推送事件
        Sinks.Many<StreamEvent> sink = Sinks.many().unicast().onBackpressureBuffer();
        
        // 异步执行任务
        new Thread(() -> {
            try {
                // 发送开始事件
                sink.tryEmitNext(new StreamEvent(StreamEventType.START.getType(), "开始执行任务：" + task));
                
                StringBuilder messages = new StringBuilder();
                messages.append(buildSystemPrompt(role));
                messages.append("\n\n任务：").append(task);
                messages.append("\n\n开始执行:\n");
                
                List<String> thoughts = new ArrayList<>();
                int retryCount = 0;
                String finalAnswer = null;
                
                for (int i = 0; i < maxIterations && finalAnswer == null; i++) {
                    log.info("ReAct 流式迭代 {}/{}, 重试：{}", i + 1, maxIterations, retryCount);
                    
                    // 调用 LLM 获取下一步
                    String response = chatModel.generate(messages.toString());
                    log.debug("LLM 响应：{}", response);
                    
                    // 解析思考
                    String thought = extractThought(response);
                    if (thought != null) {
                        thoughts.add(thought);
                        log.info("思考：{}", thought);
                        sink.tryEmitNext(new StreamEvent(StreamEventType.THOUGHT.getType(), thought));
                    }
                    
                    // 解析行动
                    String[] action = extractAction(response);
                    if (action != null) {
                        String toolName = action[0];
                        String toolInput = action[1];
                        
                        log.info("行动：调用工具 {}，输入：{}", toolName, toolInput);
                        
                        // 发送行动事件
                        Map<String, Object> actionData = new HashMap<>();
                        actionData.put("tool", toolName);
                        actionData.put("input", toolInput);
                        sink.tryEmitNext(new StreamEvent(StreamEventType.ACTION.getType(), 
                            "调用工具：" + toolName, actionData));
                        
                        // 执行工具（带重试）
                        ToolExecutionResult result = executeWithRetry(toolName, toolInput, retryCount);
                        
                        if (!result.isSuccess() && retryCount < maxRetries) {
                            retryCount++;
                            log.warn("工具执行失败，重试 {}/{}", retryCount, maxRetries);
                            messages.append("\nObservation: 工具执行失败：" + result.getError() + "，请重试或尝试其他方法\n");
                            continue;
                        }
                        
                        retryCount = 0;
                        String observation = result.isSuccess() ? result.getOutput() : "错误：" + result.getError();
                        log.info("观察：{}", observation);
                        
                        // 发送观察事件
                        Map<String, Object> obsData = new HashMap<>();
                        obsData.put("success", result.isSuccess());
                        sink.tryEmitNext(new StreamEvent(StreamEventType.OBSERVATION.getType(), observation, obsData));
                        
                        // 添加到消息历史
                        messages.append("\nThought: ").append(thought != null ? thought : "分析中...");
                        messages.append("\nAction: ").append(toolName);
                        messages.append("\nAction Input: ").append(toolInput);
                        messages.append("\nObservation: ").append(observation);
                        
                    } else {
                        // 检查是否有最终答案
                        finalAnswer = extractFinalAnswer(response);
                        if (finalAnswer != null) {
                            log.info("最终答案：{}", finalAnswer);
                            sink.tryEmitNext(new StreamEvent(StreamEventType.FINAL_ANSWER.getType(), finalAnswer));
                        } else {
                            // 如果没有行动也没有答案，让 LLM 继续思考
                            messages.append("\n").append(response);
                        }
                    }
                }
                
                if (finalAnswer == null) {
                    log.warn("达到最大迭代次数");
                    String conclusion = "经过分析，" + (thoughts.isEmpty() ? "我无法完成该任务。" : thoughts.get(thoughts.size() - 1));
                    sink.tryEmitNext(new StreamEvent(StreamEventType.FINAL_ANSWER.getType(), conclusion));
                }
                
                // 发送结束事件
                sink.tryEmitNext(new StreamEvent(StreamEventType.END.getType(), "执行完成"));
                sink.tryEmitComplete();
                
            } catch (Exception e) {
                log.error("ReAct 流式执行失败：{}", e.getMessage(), e);
                sink.tryEmitNext(new StreamEvent(StreamEventType.ERROR.getType(), "执行失败：" + e.getMessage()));
                sink.tryEmitComplete();
            }
        }).start();
        
        return sink.asFlux();
    }
}
