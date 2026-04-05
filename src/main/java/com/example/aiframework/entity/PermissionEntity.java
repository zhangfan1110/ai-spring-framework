package com.example.aiframework.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 权限实体
 */
@TableName("sys_permission")
public class PermissionEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 权限编码
     */
    private String permissionCode;

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 权限类型：1-菜单 2-按钮 3-接口
     */
    private Integer type;

    /**
     * 父级 ID
     */
    private String parentId;

    /**
     * 路径
     */
    private String path;

    /**
     * 描述
     */
    private String description;

    /**
     * 状态：0-禁用 1-正常
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getPermissionCode() { return permissionCode; }
    public void setPermissionCode(String permissionCode) { this.permissionCode = permissionCode; }
    
    public String getPermissionName() { return permissionName; }
    public void setPermissionName(String permissionName) { this.permissionName = permissionName; }
    
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
