package com.example.aiframework.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aiframework.agent.entity.ChainTemplateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 调用链模板 Mapper
 */
@Mapper
public interface ChainTemplateMapper extends BaseMapper<ChainTemplateEntity> {
    
    /**
     * 查询所有启用的模板
     */
    List<ChainTemplateEntity> selectAllEnabled();
    
    /**
     * 根据分类查询模板
     */
    List<ChainTemplateEntity> selectByCategory(@Param("category") String category);
    
    /**
     * 搜索模板
     */
    List<ChainTemplateEntity> searchByNameOrDescription(@Param("keyword") String keyword);
    
    /**
     * 获取所有分类
     */
    List<String> selectDistinctCategories();
    
    /**
     * 增加使用次数
     */
    int incrementUsageCount(@Param("id") String id);
}
