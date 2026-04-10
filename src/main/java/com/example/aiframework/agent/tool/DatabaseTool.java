package com.example.aiframework.agent.tool;

import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * 数据库查询工具 - 支持 MySQL/PostgreSQL/SQLite
 */
public class DatabaseTool implements Tool {
    
    @Override
    public String getName() {
        return "database";
    }
    
    @Override
    public String getDescription() {
        return "执行 SQL 查询，支持 SELECT/INSERT/UPDATE/DELETE。注意：DROP/TRUNCATE 等危险操作被禁止";
    }
    
    @Override
    public List<ToolParameter> getParameters() {
        return Arrays.asList(
            ToolParameter.string("url", "数据库 JDBC URL，如：jdbc:mysql://localhost:3306/dbname", true),
            ToolParameter.string("username", "数据库用户名", false),
            ToolParameter.string("password", "数据库密码", false),
            ToolParameter.string("sql", "SQL 语句", true),
            ToolParameter.string("driver", "JDBC 驱动类名，默认自动识别", false)
        );
    }
    
    @Override
    public ToolExecutionResult execute(List<ToolParameter> parameters) {
        String url = null;
        String username = "";
        String password = "";
        String sql = null;
        String driver = null;
        
        for (ToolParameter param : parameters) {
            String name = param.getName();
            Object value = param.getDefaultValue();
            
            if (value == null) continue;
            
            switch (name) {
                case "url":
                    url = value.toString();
                    break;
                case "username":
                    username = value.toString();
                    break;
                case "password":
                    password = value.toString();
                    break;
                case "sql":
                    sql = value.toString();
                    break;
                case "driver":
                    driver = value.toString();
                    break;
            }
        }
        
        if (url == null || url.trim().isEmpty()) {
            return ToolExecutionResult.error("数据库 URL 不能为空");
        }
        
        if (sql == null || sql.trim().isEmpty()) {
            return ToolExecutionResult.error("SQL 语句不能为空");
        }
        
        // 安全检查
        String sqlCheck = sql.toUpperCase().trim();
        if (sqlCheck.startsWith("DROP") || sqlCheck.startsWith("TRUNCATE") || 
            sqlCheck.startsWith("ALTER") || sqlCheck.startsWith("CREATE DATABASE") ||
            sqlCheck.contains("; DROP") || sqlCheck.contains("; TRUNCATE")) {
            return ToolExecutionResult.error("安全限制：禁止执行 DROP/TRUNCATE/ALTER 等危险操作");
        }
        
        Connection conn = null;
        try {
            // 加载驱动
            if (driver != null && !driver.trim().isEmpty()) {
                Class.forName(driver);
            } else {
                // 自动识别驱动
                if (url.contains(":mysql:")) {
                    driver = "com.mysql.cj.jdbc.Driver";
                } else if (url.contains(":postgresql:")) {
                    driver = "org.postgresql.Driver";
                } else if (url.contains(":sqlite:")) {
                    driver = "org.sqlite.JDBC";
                }
                if (driver != null) {
                    Class.forName(driver);
                }
            }
            
            // 建立连接
            Properties props = new Properties();
            if (username != null && !username.isEmpty()) {
                props.setProperty("user", username);
            }
            if (password != null) {
                props.setProperty("password", password);
            }
            conn = DriverManager.getConnection(url, props);
            
            StringBuilder result = new StringBuilder();
            
            // 执行 SQL
            if (sqlCheck.startsWith("SELECT") || sqlCheck.startsWith("SHOW") || 
                sqlCheck.startsWith("DESCRIBE") || sqlCheck.startsWith("EXPLAIN")) {
                // 查询语句
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    // 输出列名
                    result.append("列：");
                    for (int i = 1; i <= columnCount; i++) {
                        result.append(metaData.getColumnName(i));
                        if (i < columnCount) result.append(" | ");
                    }
                    result.append("\n");
                    result.append("-".repeat(80)).append("\n");
                    
                    // 输出数据
                    int rowCount = 0;
                    while (rs.next()) {
                        for (int i = 1; i <= columnCount; i++) {
                            result.append(rs.getString(i));
                            if (i < columnCount) result.append(" | ");
                        }
                        result.append("\n");
                        rowCount++;
                        
                        // 限制输出行数
                        if (rowCount >= 100) {
                            result.append("\n... (仅显示前 100 行)");
                            break;
                        }
                    }
                    
                    result.append("\n共 ").append(rowCount).append(" 行");
                }
            } else {
                // 更新语句
                try (Statement stmt = conn.createStatement()) {
                    int affected = stmt.executeUpdate(sql);
                    result.append("执行成功，影响行数：").append(affected);
                }
            }
            
            return ToolExecutionResult.success(result.toString());
            
        } catch (ClassNotFoundException e) {
            return ToolExecutionResult.error("JDBC 驱动未找到：" + driver + "，请确保已添加对应依赖");
        } catch (SQLException e) {
            return ToolExecutionResult.error("数据库操作失败：" + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // 忽略
                }
            }
        }
    }
}
