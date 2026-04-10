package com.example.aiframework.agent.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiframework.agent.entity.ChainTemplateEntity;
import com.example.aiframework.agent.entity.ChainTemplateNodeEntity;
import com.example.aiframework.agent.mapper.ChainTemplateMapper;
import com.example.aiframework.agent.mapper.ChainTemplateNodeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 调用链模板仓库
 */
@Repository
public class ChainTemplateRepository {
    
    @Autowired
    private ChainTemplateMapper templateMapper;
    
    @Autowired
    private ChainTemplateNodeMapper nodeMapper;
    
    /**
     * 查询所有启用的模板
     */
    public List<ChainTemplateEntity> findAllEnabled() {
        return templateMapper.selectAllEnabled();
    }
    
    /**
     * 根据 ID 查询模板
     */
    public ChainTemplateEntity findById(String id) {
        return templateMapper.selectById(id);
    }
    
    /**
     * 根据分类查询模板
     */
    public List<ChainTemplateEntity> findByCategory(String category) {
        return templateMapper.selectByCategory(category);
    }
    
    /**
     * 搜索模板
     */
    public List<ChainTemplateEntity> search(String keyword) {
        return templateMapper.searchByNameOrDescription(keyword);
    }
    
    /**
     * 获取所有分类
     */
    public List<String> findAllCategories() {
        return templateMapper.selectDistinctCategories();
    }
    
    /**
     * 保存模板
     */
    public ChainTemplateEntity save(ChainTemplateEntity entity) {
        if (entity.getId() == null) {
            templateMapper.insert(entity);
        } else {
            templateMapper.updateById(entity);
        }
        return entity;
    }
    
    /**
     * 删除模板
     */
    public int deleteById(String id) {
        return templateMapper.deleteById(id);
    }
    
    /**
     * 查询模板的所有节点
     */
    public List<ChainTemplateNodeEntity> findNodesByTemplateId(String templateId) {
        LambdaQueryWrapper<ChainTemplateNodeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChainTemplateNodeEntity::getTemplateId, templateId)
               .orderByAsc(ChainTemplateNodeEntity::getSortOrder);
        return nodeMapper.selectList(wrapper);
    }
    
    /**
     * 保存节点
     */
    public ChainTemplateNodeEntity saveNode(ChainTemplateNodeEntity node) {
        if (node.getId() == null) {
            nodeMapper.insert(node);
        } else {
            nodeMapper.updateById(node);
        }
        return node;
    }
    
    /**
     * 删除模板的所有节点
     */
    public int deleteNodesByTemplateId(String templateId) {
        LambdaQueryWrapper<ChainTemplateNodeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChainTemplateNodeEntity::getTemplateId, templateId);
        return nodeMapper.delete(wrapper);
    }
    
    /**
     * 批量保存节点
     */
    public int batchSaveNodes(List<ChainTemplateNodeEntity> nodes) {
        int count = 0;
        for (ChainTemplateNodeEntity node : nodes) {
            nodeMapper.insert(node);
            count++;
        }
        return count;
    }
}
