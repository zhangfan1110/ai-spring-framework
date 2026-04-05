package com.example.aiframework.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 角色实体
 */
@TableName("sys_role")
public class RoleEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 角色名称
     */
    private String roleName;

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
    
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
