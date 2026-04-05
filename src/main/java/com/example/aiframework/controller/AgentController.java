package com.example.aiframework.controller;

import com.example.aiframework.dto.ChainTemplate;
import com.example.aiframework.dto.TemplateNode;
import com.example.aiframework.service.*;
import com.example.aiframework.service.ReActAgentService.AgentRole;
import com.example.aiframework.service.ReActAgentService.CollaborationSession;
import com.example.aiframework.service.ReActAgentService.StreamEvent;
import com.example.aiframework.service.ReActAgentService.ToolChain;
import com.example.aiframework.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * Agent 能力接口
 * 
 * Controller 职责：
 * - 接收 HTTP 请求参数
 * - 参数校验
 * - 调用 Service 处理业务
 * - 封装 HTTP 响应
 * 
 * 业务逻辑全部在 Service 中实现
 */
@RestController
@RequestMapping("/api/agent")
@Tag(name = "Agent 能力", description = "ReAct 推理 Agent、工具调用链、多 Agent 协作")
public class AgentController {
    
    private static final Logger log = LoggerFactory.getLogger(AgentController.class);
    
    @Autowired
    private ReActAgentService agentService;
    
    @Autowired
    private AgentChainService chainService;
    
    @Autowired
    private ChainValidationService validationService;
    
    @Autowired
    private ChainTemplateService templateService;
    
    @Autowired
    private AgentTaskService taskService;
    
    @Autowired
    private ChainOptimizationService optimizationService;
    
    @Autowired
    private ToolTestService toolTestService;
    
    @Autowired(required = false)
    private ReActContextService contextService;
    
    // ========== ReAct 执行 ==========
    
    @Operation(summary = "ReAct 执行", description = "使用 ReAct 模式执行任务（自动推理 + 工具调用）")
    @PostMapping("/react/execute")
    public Result<Map<String, Object>> execute(@RequestBody Map<String, String> request) {
        String task = request.get("task");
        String role = request.get("role");
        
        if (task == null || task.trim().isEmpty()) {
            return Result.error("任务不能为空");
        }
        
        try {
            long startTime = System.currentTimeMillis();
            
            String result;
            if (role != null && !role.isEmpty()) {
                AgentRole agentRole = AgentRole.valueOf(role.toUpperCase());
                result = agentService.executeWithRole(task, agentRole);
            } else {
                result = agentService.execute(task);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("task", task);
            response.put("result", result);
            response.put("duration", duration);
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("ReAct 执行失败：{}", e.getMessage(), e);
            return Result.error("执行失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "ReAct 流式执行", description = "流式输出 ReAct 执行过程（思考→行动→观察→结论）")
    @PostMapping(value = "/react/execute/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<StreamEvent> executeStream(@RequestBody Map<String, String> request) {
        String task = request.get("task");
        String role = request.get("role");
        
        if (task == null || task.trim().isEmpty()) {
            // 错误情况下发送错误事件并结束
            return Flux.error(new IllegalArgumentException("任务不能为空"));
        }
        
        log.info("ReAct 流式执行：{}", task);
        
        if (role != null && !role.isEmpty()) {
            try {
                AgentRole agentRole = AgentRole.valueOf(role.toUpperCase());
                return agentService.executeStreamWithRole(task, agentRole);
            } catch (Exception e) {
                return Flux.error(e);
            }
        } else {
            return agentService.executeStream(task);
        }
    }
    
    // ========== 工具调用链 ==========
    
    @Operation(summary = "执行工具调用链", description = "按顺序执行多个工具，支持依赖关系")
    @PostMapping("/chain/execute")
    public Result<Map<String, Object>> executeChain(@RequestBody Map<String, Object> request) {
        try {
            ToolChain chain = buildChainFromRequest(request);
            
            long startTime = System.currentTimeMillis();
            Map<String, Object> result = agentService.executeChain(chain);
            long duration = System.currentTimeMillis() - startTime;
            
            result.put("duration", duration);
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("工具调用链执行失败：{}", e.getMessage(), e);
            return Result.error("执行失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "创建预设调用链", description = "根据任务描述自动生成工具调用链")
    @PostMapping("/chain/create")
    public Result<Map<String, Object>> createChain(@RequestBody Map<String, String> request) {
        String task = request.get("task");
        
        if (task == null || task.trim().isEmpty()) {
            return Result.error("任务不能为空");
        }
        
        try {
            Map<String, Object> chain = chainService.generateChain(task);
            
            Map<String, Object> response = new HashMap<>();
            response.put("task", task);
            response.put("chain", chain);
            response.put("message", "调用链已自动生成，请检查后执行");
            response.put("hint", "可调用 /api/agent/chain/execute 执行此调用链");
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("自动生成调用链失败：{}", e.getMessage(), e);
            return Result.error("生成失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "验证调用链", description = "验证工具调用链的合法性、完整性、可执行性")
    @PostMapping("/chain/validate")
    public Result<Map<String, Object>> validateChain(@RequestBody Map<String, Object> request) {
        log.info("验证调用链");
        
        try {
            ChainValidationService.ValidationResult result = validationService.validateChain(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", result.isValid());
            response.put("nodeCount", result.getNodeCount());
            response.put("estimatedDuration", result.getEstimatedDuration());
            response.put("errors", result.getErrors());
            response.put("warnings", result.getWarnings());
            response.put("details", result.getDetails());
            
            String message = result.isValid() ? "验证通过" : "验证失败：" + result.getErrors().size() + " 个错误";
            return result.isValid() ? Result.success(message, response) : Result.error(message, response);
            
        } catch (Exception e) {
            log.error("验证调用链失败：{}", e.getMessage(), e);
            return Result.error("验证失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "获取所有模板", description = "获取所有预设的调用链模板")
    @GetMapping("/chain/templates")
    public Result<List<Map<String, Object>>> listTemplates() {
        log.info("获取所有模板");
        
        List<Map<String, Object>> templates = new ArrayList<>();
        for (ChainTemplate template : templateService.getAllTemplates()) {
            Map<String, Object> templateInfo = new HashMap<>();
            templateInfo.put("id", template.getId());
            templateInfo.put("name", template.getName());
            templateInfo.put("description", template.getDescription());
            templateInfo.put("category", template.getCategory());
            templateInfo.put("estimatedDuration", template.getEstimatedDuration());
            templateInfo.put("difficulty", template.getDifficulty());
            templateInfo.put("nodeCount", template.getNodes().size());
            templates.add(templateInfo);
        }
        
        return Result.success(templates);
    }
    
    @Operation(summary = "获取模板详情", description = "获取指定模板的详细信息")
    @GetMapping("/chain/templates/{templateId}")
    public Result<Map<String, Object>> getTemplateDetail(@PathVariable String templateId) {
        log.info("获取模板详情：{}", templateId);
        
        ChainTemplate template = templateService.getTemplate(templateId);
        if (template == null) {
            return Result.error("模板不存在：" + templateId);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", template.getId());
        response.put("name", template.getName());
        response.put("description", template.getDescription());
        response.put("category", template.getCategory());
        response.put("estimatedDuration", template.getEstimatedDuration());
        response.put("difficulty", template.getDifficulty());
        
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (TemplateNode node : template.getNodes()) {
            Map<String, Object> nodeInfo = new HashMap<>();
            nodeInfo.put("stepId", node.getStepId());
            nodeInfo.put("toolName", node.getToolName());
            nodeInfo.put("description", node.getDescription());
            nodeInfo.put("parameters", node.getParameters());
            nodeInfo.put("dependsOn", node.getDependsOn());
            nodeInfo.put("parallel", node.isParallel());
            nodes.add(nodeInfo);
        }
        response.put("nodes", nodes);
        
        return Result.success(response);
    }
    
    @Operation(summary = "从模板创建调用链", description = "基于预设模板创建可执行的调用链")
    @PostMapping("/chain/from-template")
    public Result<Map<String, Object>> createFromTemplate(
            @PathVariable String templateId,
            @RequestBody(required = false) Map<String, String> params) {
        log.info("从模板创建调用链：{}", templateId);
        
        ChainTemplate template = templateService.getTemplate(templateId);
        if (template == null) {
            return Result.error("模板不存在：" + templateId);
        }
        
        try {
            Map<String, Object> chain = templateService.templateToChain(template, params);
            
            Map<String, Object> response = new HashMap<>();
            response.put("templateId", templateId);
            response.put("templateName", template.getName());
            response.put("chain", chain);
            response.put("message", "调用链已创建，可调用 /api/agent/chain/validate 验证后执行");
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("从模板创建调用链失败：{}", e.getMessage(), e);
            return Result.error("创建失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "获取模板分类", description = "获取所有模板分类")
    @GetMapping("/chain/templates/categories")
    public Result<List<String>> getCategories() {
        return Result.success(templateService.getCategories());
    }
    
    @Operation(summary = "搜索模板", description = "根据关键词搜索模板")
    @GetMapping("/chain/templates/search")
    public Result<List<Map<String, Object>>> searchTemplates(@RequestParam String keyword) {
        log.info("搜索模板：{}", keyword);
        
        List<Map<String, Object>> results = new ArrayList<>();
        for (ChainTemplate template : templateService.searchTemplates(keyword)) {
            Map<String, Object> templateInfo = new HashMap<>();
            templateInfo.put("id", template.getId());
            templateInfo.put("name", template.getName());
            templateInfo.put("description", template.getDescription());
            templateInfo.put("category", template.getCategory());
            results.add(templateInfo);
        }
        
        return Result.success(results);
    }
    
    // ========== 多 Agent 协作 ==========
    
    @Operation(summary = "创建协作会话", description = "创建多 Agent 协作会话")
    @PostMapping("/collaboration/create")
    public Result<Map<String, Object>> createCollaboration(@RequestBody Map<String, Object> request) {
        String task = (String) request.get("task");
        List<String> roles = (List<String>) request.get("roles");
        
        if (task == null || task.trim().isEmpty()) {
            return Result.error("任务不能为空");
        }
        
        if (roles == null || roles.isEmpty()) {
            roles = Arrays.asList("PLANNER", "RESEARCHER", "ANALYST", "WRITER");
        }
        
        try {
            List<AgentRole> agentRoles = new ArrayList<>();
            for (String role : roles) {
                agentRoles.add(AgentRole.valueOf(role.toUpperCase()));
            }
            
            CollaborationSession session = agentService.createCollaborationSession(task, agentRoles);
            
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", session.getSessionId());
            response.put("task", task);
            response.put("agents", session.getAgents().size());
            response.put("agentRoles", session.getAgents().values().stream()
                .map(AgentRole::getDisplayName)
                .toList());
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("创建协作会话失败：{}", e.getMessage(), e);
            return Result.error("创建失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "执行协作会话", description = "执行多 Agent 协作会话")
    @PostMapping("/collaboration/execute/{sessionId}")
    public Result<Map<String, Object>> executeCollaboration(
            @PathVariable String sessionId,
            @RequestBody(required = false) Map<String, Object> request) {
        try {
            String task = request != null ? (String) request.get("task") : "协作任务";
            List<AgentRole> roles = Arrays.asList(
                AgentRole.PLANNER,
                AgentRole.RESEARCHER,
                AgentRole.ANALYST,
                AgentRole.WRITER
            );
            
            CollaborationSession session = agentService.createCollaborationSession(task, roles);
            session.setSessionId(sessionId);
            
            long startTime = System.currentTimeMillis();
            String result = agentService.executeCollaboration(session);
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("task", task);
            response.put("result", result);
            response.put("messageHistory", session.getMessageHistory());
            response.put("duration", duration);
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("执行协作会话失败：{}", e.getMessage(), e);
            return Result.error("执行失败：" + e.getMessage());
        }
    }
    
    // ========== 工具列表 ==========
    
    @Operation(summary = "获取可用工具", description = "获取所有可用工具列表")
    @GetMapping("/tools")
    public Result<List<Map<String, Object>>> listTools() {
        List<Map<String, Object>> tools = new ArrayList<>();
        
        String[] toolNames = {
            "calculator", "datetime", "search", "file", "shell",
            "code_executor", "weather", "http", "database", "email",
            "image", "pdf", "translation", "news", "stock"
        };
        
        for (String name : toolNames) {
            Map<String, Object> tool = new HashMap<>();
            tool.put("name", name);
            tools.add(tool);
        }
        
        return Result.success(tools);
    }
    
    // ========== Agent 角色 ==========
    
    @Operation(summary = "获取 Agent 角色", description = "获取所有可用的 Agent 角色")
    @GetMapping("/roles")
    public Result<List<Map<String, String>>> listRoles() {
        List<Map<String, String>> roles = new ArrayList<>();
        
        for (AgentRole role : AgentRole.values()) {
            Map<String, String> roleInfo = new HashMap<>();
            roleInfo.put("id", role.name());
            roleInfo.put("name", role.getDisplayName());
            roleInfo.put("description", role.getDescription());
            roles.add(roleInfo);
        }
        
        return Result.success(roles);
    }
    
    // ========== 异步任务 ==========
    
    @Operation(summary = "异步执行 ReAct 任务", description = "异步执行 ReAct 任务，返回任务 ID 用于轮询状态")
    @PostMapping("/react/execute/async")
    public Result<Map<String, Object>> executeReActAsync(@RequestBody Map<String, String> request) {
        String task = request.get("task");
        String role = request.get("role");
        
        if (task == null || task.trim().isEmpty()) {
            return Result.error("任务不能为空");
        }
        
        try {
            String taskId = taskService.executeReActAsync(task, role);
            
            Map<String, Object> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("status", "PENDING");
            response.put("message", "任务已创建，可调用 /api/agent/task/{taskId}/status 查询状态");
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("创建异步任务失败：{}", e.getMessage(), e);
            return Result.error("创建任务失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "查询任务状态", description = "查询异步任务的执行状态和进度")
    @GetMapping("/task/{taskId}/status")
    public Result<Map<String, Object>> getTaskStatus(@PathVariable String taskId) {
        log.info("查询任务状态：{}", taskId);
        
        try {
            AgentTaskService.TaskInfo taskInfo = taskService.getTaskStatus(taskId);
            
            if (taskInfo == null) {
                return Result.error("任务不存在：" + taskId);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("taskId", taskInfo.getTaskId());
            response.put("status", taskInfo.getStatus().name());
            response.put("statusDescription", taskInfo.getStatus().getDescription());
            response.put("progress", taskInfo.getProgress());
            response.put("currentStep", taskInfo.getCurrentStep());
            response.put("logs", taskInfo.getLogs());
            response.put("result", taskInfo.getResult());
            response.put("errorMessage", taskInfo.getErrorMessage());
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("查询任务状态失败：{}", taskId, e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "获取任务列表", description = "获取异步任务列表，支持按状态过滤")
    @GetMapping("/tasks")
    public Result<List<Map<String, Object>>> getTaskList(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit) {
        log.info("获取任务列表：status={}, limit={}", status, limit);
        
        try {
            List<Map<String, Object>> tasks = taskService.getTaskList(status, limit);
            return Result.success(tasks);
            
        } catch (Exception e) {
            log.error("获取任务列表失败：{}", e.getMessage(), e);
            return Result.error("获取失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "取消任务", description = "取消正在执行的异步任务")
    @PostMapping("/task/{taskId}/cancel")
    public Result<Map<String, Object>> cancelTask(@PathVariable String taskId) {
        log.info("取消任务：{}", taskId);
        
        try {
            boolean success = taskService.cancelTask(taskId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("cancelled", success);
            
            return success ? Result.success(response) : Result.error("取消失败");
            
        } catch (Exception e) {
            log.error("取消任务失败：{}", taskId, e);
            return Result.error("取消失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "WebSocket 订阅任务进度", description = "通过 WebSocket 实时订阅任务进度（待实现）")
    @GetMapping(value = "/task/{taskId}/ws", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, Object>> subscribeTaskProgress(@PathVariable String taskId) {
        // TODO: 实现 WebSocket 推送
        return Flux.error(new UnsupportedOperationException("WebSocket 推送待实现"));
    }
    
    // ========== 上下文管理 ==========
    
    @Operation(summary = "带上下文执行 ReAct 任务", description = "基于历史对话和用户偏好执行任务")
    @PostMapping("/react/execute/context")
    public Result<Map<String, Object>> executeWithContext(@RequestBody Map<String, Object> request) {
        String task = (String) request.get("task");
        String role = (String) request.get("role");
        String contextId = (String) request.get("contextId");
        String sessionId = (String) request.get("sessionId");
        String userId = (String) request.get("userId");
        
        if (task == null || task.trim().isEmpty()) {
            return Result.error("任务不能为空");
        }
        
        if (contextService == null) {
            return Result.error("上下文服务未启用");
        }
        
        try {
            // 如果没有上下文 ID，创建新的
            if (contextId == null || contextId.trim().isEmpty()) {
                if (sessionId == null) {
                    sessionId = UUID.randomUUID().toString();
                }
                ReActContextService.AgentContext context = contextService.createContext(sessionId, userId);
                contextId = context.getContextId();
                log.info("创建新上下文：{}", contextId);
            }
            
            // 执行带上下文的任务
            long startTime = System.currentTimeMillis();
            ReActAgentService.AgentRole agentRole = role != null ? 
                ReActAgentService.AgentRole.valueOf(role.toUpperCase()) : null;
            
            String result = agentService.executeWithContext(task, agentRole, contextId);
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("contextId", contextId);
            response.put("result", result);
            response.put("duration", duration);
            
            // 返回上下文统计
            response.put("contextStats", contextService.getContextStats(contextId));
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("带上下文执行失败：{}", e.getMessage(), e);
            return Result.error("执行失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "创建上下文", description = "创建新的 Agent 上下文")
    @PostMapping("/context/create")
    public Result<Map<String, Object>> createContext(
            @RequestParam String sessionId,
            @RequestParam(required = false) String userId) {
        if (contextService == null) {
            return Result.error("上下文服务未启用");
        }
        
        try {
            ReActContextService.AgentContext context = contextService.createContext(sessionId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("contextId", context.getContextId());
            response.put("sessionId", context.getSessionId());
            response.put("userId", context.getUserId());
            response.put("createdAt", context.getCreatedAt());
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("创建上下文失败：{}", e.getMessage(), e);
            return Result.error("创建失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "获取上下文", description = "获取指定上下文的详细信息")
    @GetMapping("/context/{contextId}")
    public Result<Map<String, Object>> getContext(@PathVariable String contextId) {
        if (contextService == null) {
            return Result.error("上下文服务未启用");
        }
        
        ReActContextService.AgentContext context = contextService.getContext(contextId);
        if (context == null) {
            return Result.error("上下文不存在：" + contextId);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("contextId", context.getContextId());
        response.put("sessionId", context.getSessionId());
        response.put("userId", context.getUserId());
        response.put("conversationHistory", context.getConversationHistory());
        response.put("userPreferences", context.getUserPreferences());
        response.put("knowledgeBaseIds", context.getKnowledgeBaseIds());
        response.put("metadata", context.getMetadata());
        response.put("createdAt", context.getCreatedAt());
        response.put("updatedAt", context.getUpdatedAt());
        
        return Result.success(response);
    }
    
    @Operation(summary = "设置用户偏好", description = "设置用户偏好，影响后续任务执行")
    @PostMapping("/context/{contextId}/preference")
    public Result<Map<String, Object>> setUserPreference(
            @PathVariable String contextId,
            @RequestBody Map<String, Object> preferences) {
        if (contextService == null) {
            return Result.error("上下文服务未启用");
        }
        
        try {
            for (Map.Entry<String, Object> entry : preferences.entrySet()) {
                contextService.setUserPreference(contextId, entry.getKey(), entry.getValue());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("contextId", contextId);
            response.put("preferencesSet", preferences.size());
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("设置用户偏好失败：{}", e.getMessage(), e);
            return Result.error("设置失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "添加知识库", description = "添加知识库引用，用于 RAG 检索")
    @PostMapping("/context/{contextId}/knowledge")
    public Result<Map<String, Object>> addKnowledgeBase(
            @PathVariable String contextId,
            @RequestBody Map<String, String> request) {
        if (contextService == null) {
            return Result.error("上下文服务未启用");
        }
        
        String knowledgeBaseId = request.get("knowledgeBaseId");
        if (knowledgeBaseId == null) {
            return Result.error("knowledgeBaseId 不能为空");
        }
        
        try {
            contextService.addKnowledgeBase(contextId, knowledgeBaseId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("contextId", contextId);
            response.put("knowledgeBaseId", knowledgeBaseId);
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("添加知识库失败：{}", e.getMessage(), e);
            return Result.error("添加失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "获取上下文统计", description = "获取上下文的统计信息")
    @GetMapping("/context/{contextId}/stats")
    public Result<Map<String, Object>> getContextStats(@PathVariable String contextId) {
        if (contextService == null) {
            return Result.error("上下文服务未启用");
        }
        
        Map<String, Object> stats = contextService.getContextStats(contextId);
        return Result.success(stats);
    }
    
    @Operation(summary = "清空对话历史", description = "清空指定上下文的对话历史")
    @PostMapping("/context/{contextId}/clear")
    public Result<Map<String, Object>> clearHistory(@PathVariable String contextId) {
        if (contextService == null) {
            return Result.error("上下文服务未启用");
        }
        
        try {
            contextService.clearConversationHistory(contextId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("contextId", contextId);
            response.put("cleared", true);
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("清空对话历史失败：{}", e.getMessage(), e);
            return Result.error("清空失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "删除上下文", description = "删除指定上下文及其所有数据")
    @DeleteMapping("/context/{contextId}")
    public Result<Map<String, Object>> deleteContext(@PathVariable String contextId) {
        if (contextService == null) {
            return Result.error("上下文服务未启用");
        }
        
        try {
            contextService.deleteContext(contextId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("contextId", contextId);
            response.put("deleted", true);
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("删除上下文失败：{}", e.getMessage(), e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }
    
    // ========== 任务中断与恢复 ==========
    
    @Operation(summary = "中断任务", description = "中断正在执行的任务（可恢复）")
    @PostMapping("/task/{taskId}/pause")
    public Result<Map<String, Object>> pauseTask(@PathVariable String taskId) {
        log.info("中断任务：{}", taskId);
        
        try {
            boolean success = taskService.pauseTask(taskId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("paused", success);
            
            return success ? Result.success(response) : Result.error("中断失败：任务可能已完成或已取消");
            
        } catch (Exception e) {
            log.error("中断任务失败：{}", taskId, e);
            return Result.error("中断失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "恢复任务", description = "恢复已中断的任务")
    @PostMapping("/task/{taskId}/resume")
    public Result<Map<String, Object>> resumeTask(@PathVariable String taskId) {
        log.info("恢复任务：{}", taskId);
        
        try {
            boolean success = taskService.resumeTask(taskId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("resumed", success);
            
            return success ? Result.success(response) : Result.error("恢复失败：任务状态不允许恢复");
            
        } catch (Exception e) {
            log.error("恢复任务失败：{}", taskId, e);
            return Result.error("恢复失败：" + e.getMessage());
        }
    }
    
    // ========== 调用链优化 ==========
    
    @Operation(summary = "调用链优化建议", description = "分析调用链并给出优化建议")
    @PostMapping("/chain/optimize")
    public Result<Map<String, Object>> optimizeChain(@RequestBody Map<String, Object> chain) {
        log.info("分析调用链优化建议");
        
        try {
            ChainOptimizationService.OptimizationReport report = optimizationService.analyzeAndOptimize(chain);
            
            Map<String, Object> response = new HashMap<>();
            response.put("originalSteps", report.getOriginalSteps());
            response.put("optimizedSteps", report.getOptimizedSteps());
            response.put("originalDuration", report.getOriginalDuration());
            response.put("optimizedDuration", report.getOptimizedDuration());
            response.put("suggestions", report.getSuggestions());
            response.put("summary", report.getSummary());
            response.put("details", report.getDetails());
            
            int improvement = 0;
            if (report.getOriginalDuration() > 0) {
                improvement = ((report.getOriginalDuration() - report.getOptimizedDuration()) * 100) 
                    / report.getOriginalDuration();
            }
            response.put("estimatedImprovement", improvement + "%");
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("调用链优化分析失败：{}", e.getMessage(), e);
            return Result.error("分析失败：" + e.getMessage());
        }
    }
    
    // ========== 工具测试 ==========
    
    @Operation(summary = "工具执行测试", description = "测试单个工具的执行功能")
    @PostMapping("/tools/{toolName}/test")
    public Result<Map<String, Object>> testTool(
            @PathVariable String toolName,
            @RequestBody(required = false) Map<String, String> parameters) {
        log.info("测试工具：{}", toolName);
        
        try {
            ToolTestService.TestResult result = toolTestService.executeTest(toolName, parameters);
            
            Map<String, Object> response = new HashMap<>();
            response.put("toolName", result.getToolName());
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("output", result.getOutput());
            response.put("duration", result.getDuration());
            response.put("metrics", result.getMetrics());
            
            return result.isSuccess() ? Result.success(response) : Result.error("测试失败", response);
            
        } catch (Exception e) {
            log.error("工具测试失败：{}", toolName, e);
            return Result.error("测试失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "工具基准测试", description = "执行工具性能基准测试")
    @PostMapping("/tools/{toolName}/benchmark")
    public Result<Map<String, Object>> benchmarkTool(
            @PathVariable String toolName,
            @RequestParam(defaultValue = "10") int iterations,
            @RequestBody(required = false) Map<String, String> parameters) {
        log.info("基准测试工具：{}, 迭代次数：{}", toolName, iterations);
        
        try {
            ToolTestService.BenchmarkReport report = toolTestService.executeBenchmark(toolName, parameters, iterations);
            
            Map<String, Object> response = new HashMap<>();
            response.put("toolName", report.getToolName());
            response.put("iterations", report.getIterations());
            response.put("totalTime", report.getTotalTime());
            response.put("avgTime", report.getAvgTime());
            response.put("minTime", report.getMinTime());
            response.put("maxTime", report.getMaxTime());
            response.put("successCount", report.getSuccessCount());
            response.put("failCount", report.getFailCount());
            response.put("successRate", report.getSuccessRate());
            response.put("p95Time", report.getDetails().get("p95Time"));
            response.put("p99Time", report.getDetails().get("p99Time"));
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("基准测试失败：{}", toolName, e);
            return Result.error("测试失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "工具压力测试", description = "执行工具并发压力测试")
    @PostMapping("/tools/{toolName}/stress-test")
    public Result<Map<String, Object>> stressTestTool(
            @PathVariable String toolName,
            @RequestParam(defaultValue = "10") int concurrentUsers,
            @RequestParam(defaultValue = "10") int requestsPerUser,
            @RequestBody(required = false) Map<String, String> parameters) {
        log.info("压力测试工具：{}, 并发：{}, 每用户请求：{}", toolName, concurrentUsers, requestsPerUser);
        
        try {
            ToolTestService.StressTestReport report = toolTestService.executeStressTest(
                toolName, parameters, concurrentUsers, requestsPerUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("toolName", report.getToolName());
            response.put("concurrentUsers", report.getConcurrentUsers());
            response.put("totalRequests", report.getTotalRequests());
            response.put("successRequests", report.getSuccessRequests());
            response.put("failedRequests", report.getFailedRequests());
            response.put("requestsPerSecond", report.getRequestsPerSecond());
            response.put("avgResponseTime", report.getAvgResponseTime());
            response.put("p95ResponseTime", report.getP95ResponseTime());
            response.put("p99ResponseTime", report.getP99ResponseTime());
            response.put("status", report.getStatus());
            response.put("errors", report.getErrors().subList(0, Math.min(10, report.getErrors().size())));
            
            return "PASS".equals(report.getStatus()) ? Result.success(response) : Result.error("压力测试未通过", response);
            
        } catch (Exception e) {
            log.error("压力测试失败：{}", toolName, e);
            return Result.error("测试失败：" + e.getMessage());
        }
    }
    
    @Operation(summary = "批量测试所有工具", description = "测试所有可用工具的基本功能")
    @PostMapping("/tools/test-all")
    public Result<Map<String, Object>> testAllTools() {
        log.info("批量测试所有工具");
        
        try {
            Map<String, ToolTestService.TestResult> results = toolTestService.testAllTools();
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalTools", results.size());
            response.put("successCount", results.values().stream().filter(ToolTestService.TestResult::isSuccess).count());
            response.put("failCount", results.values().stream().filter(r -> !r.isSuccess()).count());
            response.put("results", results);
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("批量测试失败：{}", e.getMessage(), e);
            return Result.error("测试失败：" + e.getMessage());
        }
    }
    
    // ========== 辅助方法 ==========
    
    /**
     * 从请求构建 ToolChain 对象
     */
    @SuppressWarnings("unchecked")
    private ToolChain buildChainFromRequest(Map<String, Object> request) {
        ToolChain chain = new ToolChain();
        chain.setDescription((String) request.get("description"));
        
        List<Map<String, Object>> nodesData = (List<Map<String, Object>>) request.get("nodes");
        if (nodesData != null) {
            for (Map<String, Object> nodeData : nodesData) {
                ReActAgentService.ChainNode node = new ReActAgentService.ChainNode();
                node.setStepId((String) nodeData.get("stepId"));
                node.setToolName((String) nodeData.get("toolName"));
                node.setDependsOn((String) nodeData.get("dependsOn"));
                node.setParallel(nodeData.get("parallel") != null && (Boolean) nodeData.get("parallel"));
                
                Map<String, String> params = new HashMap<>();
                Map<String, Object> paramsData = (Map<String, Object>) nodeData.get("parameters");
                if (paramsData != null) {
                    for (Map.Entry<String, Object> entry : paramsData.entrySet()) {
                        params.put(entry.getKey(), entry.getValue().toString());
                    }
                }
                node.setParameters(params);
                
                chain.addNode(node);
            }
        }
        
        return chain;
    }
}
