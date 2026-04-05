package com.example.aiframework.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码生成器（一次性使用）
 */
public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 生成 admin123 的哈希
        String hashed = encoder.encode("admin123");
        
        System.out.println("原始密码：admin123");
        System.out.println("BCrypt 哈希：" + hashed);
        System.out.println();
        System.out.println("SQL 更新语句：");
        System.out.println("UPDATE sys_user SET password = '" + hashed + "' WHERE username = 'admin';");
    }
}
