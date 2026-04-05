package com.example.aiframework.service;

import com.example.aiframework.entity.ChainTemplateEntity;
import com.example.aiframework.entity.ChainTemplateNodeEntity;
import com.example.aiframework.repository.ChainTemplateRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * 调用链模板服务
 * 模板数据存储在 MySQL 数据库中，支持动态添加和修改
 */
@Service
public class ChainTemplateService implements CommandLineRunner {
    
    private static final Logger log = LoggerFactory.getLogger(ChainTemplateService.class);
    
    @Autowired
    private ChainTemplateRepository templateRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // 内存缓存
    private final Map<String, ChainTemplate> templatesCache = new HashMap<>();
    
    @PostConstruct
    public void init() {
        log.info("初始化模板服务");
        loadTemplatesFromDatabase();
    }
    
    @Override
    public void run(String... args) {
        ensureBuiltinTemplates();
    }
    
    private void loadTemplatesFromDatabase() {
        try {
            List<ChainTemplateEntity> entities = templateRepository.findAllEnabled();
            for (ChainTemplateEntity entity : entities) {
                ChainTemplate template = entityToTemplate(entity);
                if (template != null) {
                    templatesCache.put(entity.getId(), template);
                }
            }
            log.info("从数据库加载 {} 个模板", templatesCache.size());
        } catch (Exception e) {
            log.error("从数据库加载模板失败：{}", e.getMessage(), e);
            initBuiltinTemplates();
        }
    }
    
    private void ensureBuiltinTemplates() {
        if (templatesCache.isEmpty()) {
            log.info("数据库中没有模板，初始化内置模板...");
            initBuiltinTemplates();
        }
    }
    
    private void initBuiltinTemplates() {
        // 1. 下载并处理文件
        ChainTemplate t1 = createTemplate(
            "download_process",
            "下载并处理文件",
            "从 URL 下载文件，进行文本处理，保存结果",
            "数据处理",
            5000,
            2,
            Arrays.asList(
                createNode("step1", "http", "下载文件", map("url", "${url}", "method", "GET"), null, false, 1),
                createNode("step2", "shell", "处理文本", map("command", "echo '${step1_result}' | wc -l"), "step1", false, 2),
                createNode("step3", "file", "保存结果", map("operation", "write", "path", "${output_path}", "content", "${step2_result}"), "step2", false, 3)
            )
        );
        saveTemplateToDatabase(t1);
        
        // 2. 天气查询与提醒
        ChainTemplate t2 = createTemplate(
            "weather_notify",
            "天气查询与提醒",
            "查询天气，根据条件判断是否发送邮件提醒",
            "信息查询",
            4000,
            2,
            Arrays.asList(
                createNode("step1", "weather", "查询天气", map("city", "${city:-北京}"), null, false, 1),
                createNode("step2", "code_executor", "判断是否需要提醒", map("language", "python", "code", "print('需要带伞' if int('${step1_result}'.split()[0]) > 30 else '不需要')"), "step1", false, 2),
                createNode("step3", "email", "发送邮件提醒", map("to", "${email}", "subject", "天气提醒", "body", "${step2_result}"), "step2", false, 3)
            )
        );
        saveTemplateToDatabase(t2);
        
        // 3. 搜索并翻译
        ChainTemplate t3 = createTemplate(
            "search_translate",
            "搜索并翻译",
            "搜索网络信息，将结果翻译成中文",
            "信息查询",
            6000,
            2,
            Arrays.asList(
                createNode("step1", "search", "搜索信息", map("query", "${query}", "limit", "5"), null, false, 1),
                createNode("step2", "translation", "翻译结果", map("text", "${step1_result}", "source_lang", "en", "target_lang", "zh"), "step1", false, 2)
            )
        );
        saveTemplateToDatabase(t3);
        
        // 4. PDF 解析与分析
        ChainTemplate t4 = createTemplate(
            "pdf_analysis",
            "PDF 解析与分析",
            "解析 PDF 文件，提取文本，进行关键词分析",
            "数据处理",
            8000,
            3,
            Arrays.asList(
                createNode("step1", "pdf", "解析 PDF", map("operation", "extract", "path", "${file_path}"), null, false, 1),
                createNode("step2", "shell", "提取关键词", map("command", "echo '${step1_result}' | grep -oE '\\w+' | sort | uniq -c | sort -rn | head -20"), "step1", false, 2),
                createNode("step3", "code_executor", "生成分析报告", map("language", "python", "code", "print('分析完成')"), "step2", false, 3)
            )
        );
        saveTemplateToDatabase(t4);
        
        // 5. 新闻聚合
        ChainTemplate t5 = createTemplate(
            "news_digest",
            "新闻聚合",
            "获取多条新闻，提取标题，生成摘要",
            "信息查询",
            5000,
            2,
            Arrays.asList(
                createNode("step1", "news", "获取新闻", map("query", "${topic:-AI}", "limit", "${count:-10}"), null, false, 1),
                createNode("step2", "shell", "提取标题", map("command", "echo '${step1_result}' | head -10"), "step1", false, 2),
                createNode("step3", "file", "保存到文件", map("operation", "write", "path", "news_digest.txt", "content", "${step2_result}"), "step2", false, 3)
            )
        );
        saveTemplateToDatabase(t5);
        
        // 重新加载缓存
        loadTemplatesFromDatabase();
        log.info("内置模板初始化完成");
    }
    
    private ChainTemplate createTemplate(String id, String name, String description, 
                                         String category, int duration, int difficulty, List<TemplateNode> nodes) {
        ChainTemplate template = new ChainTemplate();
        template.setId(id);
        template.setName(name);
        template.setDescription(description);
        template.setCategory(category);
        template.setEstimatedDuration(duration);
        template.setDifficulty(difficulty);
        template.setNodes(nodes);
        return template;
    }
    
    private TemplateNode createNode(String stepId, String toolName, String description,
                                    Map<String, String> parameters, String dependsOn, 
                                    boolean parallel, int sortOrder) {
        TemplateNode node = new TemplateNode();
        node.setStepId(stepId);
        node.setToolName(toolName);
        node.setDescription(description);
        node.setParameters(parameters);
        node.setDependsOn(dependsOn);
        node.setParallel(parallel);
        node.setSortOrder(sortOrder);
        return node;
    }
    
    private Map<String, String> map(String... pairs) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put(pairs[i], pairs[i + 1]);
        }
        return m;
    }
    
    private void saveTemplateToDatabase(ChainTemplate template) {
        try {
            // 检查是否已存在
            ChainTemplateEntity existing = templateRepository.findById(template.getId());
            if (existing != null) {
                log.debug("模板已存在：{}", template.getId());
                return;
            }
            
            // 保存模板
            ChainTemplateEntity entity = new ChainTemplateEntity();
            entity.setId(template.getId());
            entity.setName(template.getName());
            entity.setDescription(template.getDescription());
            entity.setCategory(template.getCategory());
            entity.setEstimatedDuration(template.getEstimatedDuration());
            entity.setDifficulty(template.getDifficulty());
            entity.setIsEnabled(true);
            entity.setIsBuiltin(true);
            entity.setVersion(1);
            
            templateRepository.save(entity);
            
            // 保存节点
            for (TemplateNode node : template.getNodes()) {
                ChainTemplateNodeEntity nodeEntity = new ChainTemplateNodeEntity();
                nodeEntity.setId(template.getId() + "_" + node.getStepId());
                nodeEntity.setTemplateId(template.getId());
                nodeEntity.setStepId(node.getStepId());
                nodeEntity.setToolName(node.getToolName());
                nodeEntity.setDescription(node.getDescription());
                nodeEntity.setParameters(objectMapper.writeValueAsString(node.getParameters()));
                nodeEntity.setDependsOn(node.getDependsOn());
                nodeEntity.setIsParallel(node.isParallel());
                nodeEntity.setSortOrder(node.getSortOrder());
                
                templateRepository.saveNode(nodeEntity);
            }
            
            log.info("保存模板到数据库：{} - {}", template.getId(), template.getName());
            
        } catch (Exception e) {
            log.error("保存模板到数据库失败：{}", template.getId(), e);
        }
    }
    
    private ChainTemplate entityToTemplate(ChainTemplateEntity entity) {
        try {
            ChainTemplate template = new ChainTemplate();
            template.setId(entity.getId());
            template.setName(entity.getName());
            template.setDescription(entity.getDescription());
            template.setCategory(entity.getCategory());
            template.setEstimatedDuration(entity.getEstimatedDuration());
            template.setDifficulty(entity.getDifficulty());
            
            List<ChainTemplateNodeEntity> nodeEntities = templateRepository.findNodesByTemplateId(entity.getId());
            List<TemplateNode> nodes = new ArrayList<>();
            
            for (ChainTemplateNodeEntity nodeEntity : nodeEntities) {
                TemplateNode node = new TemplateNode();
                node.setStepId(nodeEntity.getStepId());
                node.setToolName(nodeEntity.getToolName());
                node.setDescription(nodeEntity.getDescription());
                node.setDependsOn(nodeEntity.getDependsOn());
                node.setParallel(nodeEntity.getIsParallel() != null && nodeEntity.getIsParallel());
                node.setSortOrder(nodeEntity.getSortOrder());
                
                if (nodeEntity.getParameters() != null) {
                    node.setParameters(objectMapper.readValue(
                        nodeEntity.getParameters(), 
                        new TypeReference<Map<String, String>>() {}
                    ));
                }
                
                nodes.add(node);
            }
            
            template.setNodes(nodes);
            return template;
            
        } catch (Exception e) {
            log.error("转换模板实体失败：{}", entity.getId(), e);
            return null;
        }
    }
    
    // ========== 公共方法 ==========
    
    public List<ChainTemplate> getAllTemplates() {
        return new ArrayList<>(templatesCache.values());
    }
    
    public ChainTemplate getTemplate(String templateId) {
        return templatesCache.get(templateId);
    }
    
    public List<ChainTemplate> getTemplatesByCategory(String category) {
        List<ChainTemplate> result = new ArrayList<>();
        for (ChainTemplate template : templatesCache.values()) {
            if (template.getCategory().equals(category)) {
                result.add(template);
            }
        }
        return result;
    }
    
    public List<String> getCategories() {
        Set<String> categories = new HashSet<>();
        for (ChainTemplate template : templatesCache.values()) {
            categories.add(template.getCategory());
        }
        return new ArrayList<>(categories);
    }
    
    public List<ChainTemplate> searchTemplates(String keyword) {
        List<ChainTemplate> result = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();
        
        for (ChainTemplate template : templatesCache.values()) {
            if (template.getName().toLowerCase().contains(lowerKeyword) ||
                template.getDescription().toLowerCase().contains(lowerKeyword) ||
                template.getId().toLowerCase().contains(lowerKeyword)) {
                result.add(template);
            }
        }
        return result;
    }
    
    public Map<String, Object> templateToChain(ChainTemplate template, Map<String, String> userParams) {
        Map<String, Object> chain = new HashMap<>();
        chain.put("description", template.getName() + " - " + template.getDescription());
        
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (TemplateNode templateNode : template.getNodes()) {
            Map<String, Object> node = new HashMap<>();
            node.put("stepId", templateNode.getStepId());
            node.put("toolName", templateNode.getToolName());
            node.put("dependsOn", templateNode.getDependsOn());
            node.put("parallel", templateNode.isParallel());
            
            Map<String, String> parameters = new HashMap<>();
            if (templateNode.getParameters() != null) {
                for (Map.Entry<String, String> entry : templateNode.getParameters().entrySet()) {
                    String value = entry.getValue();
                    if (userParams != null) {
                        for (Map.Entry<String, String> param : userParams.entrySet()) {
                            value = value.replace("${" + param.getKey() + "}", param.getValue());
                        }
                    }
                    parameters.put(entry.getKey(), value);
                }
            }
            node.put("parameters", parameters);
            nodes.add(node);
        }
        
        chain.put("nodes", nodes);
        return chain;
    }
    
}
