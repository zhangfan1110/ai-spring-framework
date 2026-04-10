package com.example.aiframework.agent.tool;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * 邮件发送工具 - 支持 SMTP 协议
 */
public class EmailTool implements Tool {
    
    @Override
    public String getName() {
        return "email";
    }
    
    @Override
    public String getDescription() {
        return "发送邮件，支持 HTML 格式。需要配置 SMTP 服务器信息";
    }
    
    @Override
    public List<ToolParameter> getParameters() {
        return Arrays.asList(
            ToolParameter.string("smtpHost", "SMTP 服务器地址，如：smtp.gmail.com", true),
            ToolParameter.number("smtpPort", "SMTP 端口，默认 587", false),
            ToolParameter.string("username", "发件人邮箱账号", true),
            ToolParameter.string("password", "发件人邮箱密码/授权码", true),
            ToolParameter.string("from", "发件人邮箱地址，默认同 username", false),
            ToolParameter.string("to", "收件人邮箱地址，多个用逗号分隔", true),
            ToolParameter.string("subject", "邮件主题", true),
            ToolParameter.string("content", "邮件内容", true),
            ToolParameter.bool("isHtml", "是否为 HTML 格式，默认 false", false)
        );
    }
    
    @Override
    public ToolExecutionResult execute(List<ToolParameter> parameters) {
        String smtpHost = null;
        int smtpPort = 587;
        String username = null;
        String password = null;
        String from = null;
        String to = null;
        String subject = null;
        String content = null;
        boolean isHtml = false;
        
        for (ToolParameter param : parameters) {
            String name = param.getName();
            Object value = param.getDefaultValue();
            
            if (value == null) continue;
            
            switch (name) {
                case "smtpHost":
                    smtpHost = value.toString();
                    break;
                case "smtpPort":
                    try {
                        smtpPort = Integer.parseInt(value.toString());
                    } catch (NumberFormatException e) {
                        smtpPort = 587;
                    }
                    break;
                case "username":
                    username = value.toString();
                    break;
                case "password":
                    password = value.toString();
                    break;
                case "from":
                    from = value.toString();
                    break;
                case "to":
                    to = value.toString();
                    break;
                case "subject":
                    subject = value.toString();
                    break;
                case "content":
                    content = value.toString();
                    break;
                case "isHtml":
                    isHtml = Boolean.parseBoolean(value.toString());
                    break;
            }
        }
        
        // 参数校验
        if (smtpHost == null || smtpHost.trim().isEmpty()) {
            return ToolExecutionResult.error("SMTP 服务器地址不能为空");
        }
        if (username == null || username.trim().isEmpty()) {
            return ToolExecutionResult.error("发件人账号不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return ToolExecutionResult.error("发件人密码不能为空");
        }
        if (to == null || to.trim().isEmpty()) {
            return ToolExecutionResult.error("收件人地址不能为空");
        }
        if (subject == null || subject.trim().isEmpty()) {
            return ToolExecutionResult.error("邮件主题不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            return ToolExecutionResult.error("邮件内容不能为空");
        }
        
        if (from == null || from.trim().isEmpty()) {
            from = username;
        }
        
        try {
            // 使用 JavaMail 发送
            Class<?> sessionClass = Class.forName("jakarta.mail.Session");
            Object session = sessionClass.getMethod("getInstance", Properties.class)
                .invoke(null, createProperties(smtpHost, smtpPort, username, password));
            
            // 创建消息
            Class<?> messageClass = Class.forName("jakarta.mail.internet.MimeMessage");
            Object message = messageClass.getConstructor(sessionClass).newInstance(session);
            
            // 设置发件人
            Class<?> internetAddressClass = Class.forName("jakarta.mail.internet.InternetAddress");
            Object fromAddress = internetAddressClass.getConstructor(String.class).newInstance(from);
            message.getClass().getMethod("setFrom", internetAddressClass).invoke(message, fromAddress);
            
            // 设置收件人
            String[] recipients = to.split(",");
            Class<?> recipientType = Class.forName("jakarta.mail.Message$RecipientType");
            Object toType = recipientType.getField("TO").get(null);
            for (String recipient : recipients) {
                Object toAddress = internetAddressClass.getConstructor(String.class)
                    .newInstance(recipient.trim());
                message.getClass().getMethod("addRecipient", recipientType, internetAddressClass)
                    .invoke(message, toType, toAddress);
            }
            
            // 设置主题
            message.getClass().getMethod("setSubject", String.class).invoke(message, subject);
            
            // 设置内容
            message.getClass().getMethod("setText", String.class, String.class)
                .invoke(message, content, isHtml ? "html" : "plain");
            
            // 发送
            Class<?> transportClass = Class.forName("jakarta.mail.Transport");
            Object transport = session.getClass().getMethod("getTransport", String.class)
                .invoke(session, "smtp");
            transport.getClass().getMethod("connect", String.class, int.class, String.class, String.class)
                .invoke(transport, smtpHost, smtpPort, username, password);
            transport.getClass().getMethod("sendMessage", 
                Class.forName("jakarta.mail.Message"), 
                Class.forName("jakarta.mail.Address").arrayType())
                .invoke(transport, message, new Object[]{new Object[]{fromAddress}});
            transport.getClass().getMethod("close").invoke(transport);
            
            return ToolExecutionResult.success("邮件发送成功!\n收件人：" + to + "\n主题：" + subject);
            
        } catch (ClassNotFoundException e) {
            return ToolExecutionResult.error("JavaMail 库未找到，请添加 jakarta.mail 依赖");
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (e.getCause() != null) {
                errorMsg = e.getCause().getMessage();
            }
            return ToolExecutionResult.error("邮件发送失败：" + errorMsg);
        }
    }
    
    private Properties createProperties(String smtpHost, int smtpPort, String username, String password) {
        Properties props = new Properties();
        props.setProperty("mail.smtp.host", smtpHost);
        props.setProperty("mail.smtp.port", String.valueOf(smtpPort));
        props.setProperty("mail.smtp.auth", "true");
        props.setProperty("mail.smtp.starttls.enable", "true");
        props.setProperty("mail.user", username);
        props.setProperty("mail.password", password);
        return props;
    }
}
