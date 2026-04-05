package com.example.aiframework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aiframework.entity.PermissionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 权限 Mapper
 */
@Mapper
public interface PermissionMapper extends BaseMapper<PermissionEntity> {
}
