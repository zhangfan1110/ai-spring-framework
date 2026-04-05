package com.example.aiframework;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI Spring Framework 启动类
 * 
 * @author 皮皮虾
 * @since 2024-03-30
 */
@SpringBootApplication
@EnableAsync  // 启用异步执行
@EnableScheduling  // 启用定时任务
public class Application {
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║   AI Spring Framework 启动成功！🦐         ║");
        System.out.println("║   LangChain4j + 通义千问                    ║");
        System.out.println("╚════════════════════════════════════════════╝");
    }
}
