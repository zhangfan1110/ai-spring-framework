package com.example.aiframework.config.data;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Druid 配置 - 手动注册监控 Servlet 和 Filter
 */
@Configuration
public class DruidConfig {

    /**
     * 配置 Druid 监控页面
     */
    @Bean
    public ServletRegistrationBean statViewServletRegistration() {
        ServletRegistrationBean registration = new ServletRegistrationBean();
        registration.setServlet(new com.alibaba.druid.support.jakarta.StatViewServlet());
        registration.addUrlMappings("/druid/*");
        
        // 初始化参数
        Map<String, String> initParams = new HashMap<>();
        initParams.put("loginUsername", "admin");
        initParams.put("loginPassword", "admin123");
        initParams.put("resetEnable", "false");
        initParams.put("allow", "127.0.0.1");
        registration.setInitParameters(initParams);
        return registration;
    }

    /**
     * 配置 Druid Web 监控 Filter
     */
    @Bean
    public FilterRegistrationBean webStatFilterRegistration() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new com.alibaba.druid.support.jakarta.WebStatFilter());
        registration.addUrlPatterns("/*");
        
        // 初始化参数
        Map<String, String> initParams = new HashMap<>();
        initParams.put("exclusions", "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*");
        initParams.put("sessionStatEnable", "true");
        
        registration.setInitParameters(initParams);
        registration.setName("druidWebStatFilter");
        
        return registration;
    }
}
