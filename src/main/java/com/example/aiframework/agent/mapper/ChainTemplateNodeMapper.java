package com.example.aiframework.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aiframework.agent.entity.ChainTemplateNodeEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 调用链模板节点 Mapper
 */
@Mapper
public interface ChainTemplateNodeMapper extends BaseMapper<ChainTemplateNodeEntity> {
}
