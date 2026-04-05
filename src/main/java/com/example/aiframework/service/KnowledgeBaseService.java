package com.example.aiframework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiframework.entity.KnowledgeBaseEntity;
import com.example.aiframework.entity.KnowledgeDocumentEntity;
import com.example.aiframework.mapper.KnowledgeBaseMapper;
import com.example.aiframework.mapper.KnowledgeDocumentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 知识库服务
 */
@Service
public class KnowledgeBaseService {

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private KnowledgeDocumentMapper documentMapper;

    /**
     * 创建知识库
     */
    @Transactional
    public KnowledgeBaseEntity createKnowledgeBase(String name, String description, String type, String createdBy) {
        KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
        kb.setId(UUID.randomUUID().toString());
        kb.setName(name);
        kb.setDescription(description);
        kb.setType(type);
        kb.setCreatedBy(createdBy);
        kb.setDocumentCount(0);
        kb.setCollectionName("kb_" + kb.getId().replace("-", "_"));
        kb.setStatus(1);
        kb.setCreateTime(LocalDateTime.now());

        knowledgeBaseMapper.insert(kb);

        // TODO: 在 Milvus 中创建对应的集合

        return kb;
    }

    /**
     * 更新知识库
     */
    public KnowledgeBaseEntity updateKnowledgeBase(String id, String name, String description) {
        KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(id);
        if (kb == null) {
            return null;
        }

        if (name != null) kb.setName(name);
        if (description != null) kb.setDescription(description);
        kb.setUpdateTime(LocalDateTime.now());

        knowledgeBaseMapper.updateById(kb);
        return kb;
    }

    /**
     * 删除知识库
     */
    @Transactional
    public boolean deleteKnowledgeBase(String id) {
        KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(id);
        if (kb == null) {
            return false;
        }

        // 删除所有文档
        LambdaQueryWrapper<KnowledgeDocumentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeDocumentEntity::getKnowledgeBaseId, id);
        documentMapper.delete(wrapper);

        // TODO: 删除 Milvus 集合

        knowledgeBaseMapper.deleteById(id);
        return true;
    }

    /**
     * 获取知识库详情
     */
    public KnowledgeBaseEntity getKnowledgeBase(String id) {
        return knowledgeBaseMapper.selectById(id);
    }

    /**
     * 查询用户知识库列表
     */
    public List<KnowledgeBaseEntity> listKnowledgeBases(String userId) {
        return knowledgeBaseMapper.findByUser(userId);
    }

    /**
     * 获取知识库统计
     */
    public Map<String, Object> getKnowledgeBaseStats(String kbId) {
        KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            return null;
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("knowledgeBase", kb);
        stats.put("documentCount", documentMapper.countByKnowledgeBase(kbId));

        return stats;
    }
}
