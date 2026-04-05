package com.example.aiframework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiframework.entity.WorkflowDefinitionEntity;
import com.example.aiframework.mapper.WorkflowDefinitionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 工作流定义服务
 */
@Service
public class WorkflowDefinitionService {

    @Autowired
    private WorkflowDefinitionMapper workflowDefinitionMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 创建工作流定义
     */
    @Transactional
    public WorkflowDefinitionEntity createWorkflow(String workflowCode, String workflowName, 
                                                   String description, String workflowConfig, String createdBy) {
        WorkflowDefinitionEntity workflow = new WorkflowDefinitionEntity();
        workflow.setId(UUID.randomUUID().toString());
        workflow.setWorkflowCode(workflowCode);
        workflow.setWorkflowName(workflowName);
        workflow.setDescription(description);
        workflow.setVersion("1.0.0");
        workflow.setWorkflowConfig(workflowConfig);
        workflow.setStatus("DRAFT");
        workflow.setCreatedBy(createdBy);
        workflow.setCreateTime(LocalDateTime.now());

        workflowDefinitionMapper.insert(workflow);
        return workflow;
    }

    /**
     * 更新工作流定义
     */
    public WorkflowDefinitionEntity updateWorkflow(String id, String workflowName, 
                                                   String description, String workflowConfig) {
        WorkflowDefinitionEntity workflow = workflowDefinitionMapper.selectById(id);
        if (workflow == null) {
            return null;
        }

        if (workflowName != null) workflow.setWorkflowName(workflowName);
        if (description != null) workflow.setDescription(description);
        if (workflowConfig != null) workflow.setWorkflowConfig(workflowConfig);
        workflow.setUpdateTime(LocalDateTime.now());

        workflowDefinitionMapper.updateById(workflow);
        return workflow;
    }

    /**
     * 发布工作流
     */
    @Transactional
    public WorkflowDefinitionEntity publishWorkflow(String id, String publishedBy) {
        WorkflowDefinitionEntity workflow = workflowDefinitionMapper.selectById(id);
        if (workflow == null) {
            return null;
        }

        // 增加版本号
        String[] parts = workflow.getVersion().split("\\.");
        parts[1] = String.valueOf(Integer.parseInt(parts[1]) + 1);
        workflow.setVersion(String.join(".", parts));

        workflow.setStatus("PUBLISHED");
        workflow.setPublishedBy(publishedBy);
        workflow.setPublishedTime(LocalDateTime.now());

        workflowDefinitionMapper.updateById(workflow);
        return workflow;
    }

    /**
     * 归档工作流
     */
    public WorkflowDefinitionEntity archiveWorkflow(String id) {
        WorkflowDefinitionEntity workflow = workflowDefinitionMapper.selectById(id);
        if (workflow == null) {
            return null;
        }

        workflow.setStatus("ARCHIVED");
        workflow.setUpdateTime(LocalDateTime.now());

        workflowDefinitionMapper.updateById(workflow);
        return workflow;
    }

    /**
     * 获取工作流定义
     */
    public WorkflowDefinitionEntity getWorkflow(String id) {
        return workflowDefinitionMapper.selectById(id);
    }

    /**
     * 查询已发布的工作流列表
     */
    public List<WorkflowDefinitionEntity> listPublishedWorkflows() {
        return workflowDefinitionMapper.findPublished();
    }

    /**
     * 根据编码查询最新版本
     */
    public WorkflowDefinitionEntity getLatestWorkflow(String code) {
        return workflowDefinitionMapper.findLatestByCode(code);
    }

    /**
     * 解析工作流配置
     */
    public WorkflowConfig parseWorkflowConfig(String configJson) {
        try {
            return objectMapper.readValue(configJson, WorkflowConfig.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 工作流配置类
     */
    public static class WorkflowConfig {
        private String startNodeId;
        private List<NodeConfig> nodes;
        private List<EdgeConfig> edges;

        public String getStartNodeId() { return startNodeId; }
        public void setStartNodeId(String startNodeId) { this.startNodeId = startNodeId; }
        
        public List<NodeConfig> getNodes() { return nodes; }
        public void setNodes(List<NodeConfig> nodes) { this.nodes = nodes; }
        
        public List<EdgeConfig> getEdges() { return edges; }
        public void setEdges(List<EdgeConfig> edges) { this.edges = edges; }
    }

    /**
     * 节点配置类
     */
    public static class NodeConfig {
        private String id;
        private String name;
        private String type; // START/END/TASK/CONDITION/PARALLEL
        private Map<String, Object> config;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public Map<String, Object> getConfig() { return config; }
        public void setConfig(Map<String, Object> config) { this.config = config; }
    }

    /**
     * 边配置类（节点间的连接）
     */
    public static class EdgeConfig {
        private String source;
        private String target;
        private String condition; // 条件表达式（用于条件分支）

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
        
        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }
    }
}
