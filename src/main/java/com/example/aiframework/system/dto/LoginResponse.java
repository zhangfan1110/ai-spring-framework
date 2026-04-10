package com.example.aiframework.system.dto;

import java.util.List;

/**
 * 登录响应
 */
public class LoginResponse {
    
    private String token;
    private String tokenType;
    private Long expiresIn;
    private UserInfo user;

    public LoginResponse(String token, UserInfo user) {
        this.token = token;
        this.tokenType = "Bearer";
        this.expiresIn = 86400L;
        this.user = user;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    
    public Long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }
    
    public UserInfo getUser() { return user; }
    public void setUser(UserInfo user) { this.user = user; }

    /**
     * 用户信息
     */
    public static class UserInfo {
        private String id;
        private String username;
        private String nickname;
        private String avatar;
        private List<String> roles;
        private List<String> permissions;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
        
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
        
        public List<String> getPermissions() { return permissions; }
        public void setPermissions(List<String> permissions) { this.permissions = permissions; }
    }
}
