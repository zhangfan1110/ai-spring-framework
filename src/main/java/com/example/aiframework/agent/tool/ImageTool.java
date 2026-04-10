package com.example.aiframework.agent.tool;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * 图片处理工具 - 支持格式转换、缩放、裁剪等
 */
public class ImageTool implements Tool {
    
    @Override
    public String getName() {
        return "image";
    }
    
    @Override
    public String getDescription() {
        return "图片处理工具，支持格式转换、缩放、裁剪、旋转、添加水印等操作";
    }
    
    @Override
    public List<ToolParameter> getParameters() {
        return Arrays.asList(
            ToolParameter.string("input", "输入图片路径或 URL", true),
            ToolParameter.string("output", "输出图片路径，默认自动生成", false),
            ToolParameter.string("operation", "操作类型：resize/crop/rotate/convert/watermark/info", true),
            ToolParameter.number("width", "宽度 (resize/crop 时使用)", false),
            ToolParameter.number("height", "高度 (resize/crop 时使用)", false),
            ToolParameter.number("angle", "旋转角度 (rotate 时使用)，默认 90", false),
            ToolParameter.string("format", "输出格式 (convert 时使用)：jpg/png/gif/webp", false),
            ToolParameter.string("watermark", "水印文字 (watermark 时使用)", false),
            ToolParameter.number("quality", "输出质量 1-100，默认 90", false)
        );
    }
    
    @Override
    public ToolExecutionResult execute(List<ToolParameter> parameters) {
        String input = null;
        String output = null;
        String operation = null;
        Integer width = null;
        Integer height = null;
        Integer angle = 90;
        String format = "jpg";
        String watermark = null;
        int quality = 90;
        
        for (ToolParameter param : parameters) {
            String name = param.getName();
            Object value = param.getDefaultValue();
            
            if (value == null) continue;
            
            switch (name) {
                case "input":
                    input = value.toString();
                    break;
                case "output":
                    output = value.toString();
                    break;
                case "operation":
                    operation = value.toString().toLowerCase();
                    break;
                case "width":
                    try {
                        width = Integer.parseInt(value.toString());
                    } catch (NumberFormatException e) { }
                    break;
                case "height":
                    try {
                        height = Integer.parseInt(value.toString());
                    } catch (NumberFormatException e) { }
                    break;
                case "angle":
                    try {
                        angle = Integer.parseInt(value.toString());
                    } catch (NumberFormatException e) { }
                    break;
                case "format":
                    format = value.toString().toLowerCase();
                    break;
                case "watermark":
                    watermark = value.toString();
                    break;
                case "quality":
                    try {
                        quality = Integer.parseInt(value.toString());
                    } catch (NumberFormatException e) { }
                    break;
            }
        }
        
        if (input == null || input.trim().isEmpty()) {
            return ToolExecutionResult.error("输入图片路径不能为空");
        }
        
        if (operation == null || operation.trim().isEmpty()) {
            return ToolExecutionResult.error("操作类型不能为空");
        }
        
        try {
            // 加载图片
            File inputFile = new File(input);
            if (!inputFile.exists()) {
                return ToolExecutionResult.error("输入文件不存在：" + input);
            }
            
            BufferedImage image = ImageIO.read(inputFile);
            if (image == null) {
                return ToolExecutionResult.error("无法读取图片，格式可能不支持");
            }
            
            BufferedImage result = image;
            
            // 执行操作
            switch (operation) {
                case "resize":
                    if (width == null || height == null) {
                        return ToolExecutionResult.error("resize 操作需要指定 width 和 height");
                    }
                    result = resizeImage(image, width, height);
                    break;
                    
                case "crop":
                    if (width == null || height == null) {
                        return ToolExecutionResult.error("crop 操作需要指定 width 和 height");
                    }
                    result = cropImage(image, 0, 0, width, height);
                    break;
                    
                case "rotate":
                    result = rotateImage(image, angle);
                    break;
                    
                case "convert":
                    // 格式转换，保持原图
                    break;
                    
                case "watermark":
                    if (watermark == null || watermark.trim().isEmpty()) {
                        return ToolExecutionResult.error("watermark 操作需要指定水印文字");
                    }
                    result = addWatermark(image, watermark);
                    break;
                    
                case "info":
                    return ToolExecutionResult.success(buildImageInfo(image, inputFile));
                    
                default:
                    return ToolExecutionResult.error("未知操作：" + operation);
            }
            
            // 保存结果
            String outputPath = output;
            if (outputPath == null || outputPath.trim().isEmpty()) {
                String baseName = inputFile.getName();
                int dotIndex = baseName.lastIndexOf(".");
                String name = dotIndex > 0 ? baseName.substring(0, dotIndex) : baseName;
                outputPath = inputFile.getParent() + "/" + name + "_processed." + format;
            }
            
            File outputFile = new File(outputPath);
            ImageIO.write(result, format, outputFile);
            
            return ToolExecutionResult.success("图片处理完成!\n操作：" + operation + "\n输出：" + outputPath + "\n尺寸：" + result.getWidth() + "x" + result.getHeight());
            
        } catch (Exception e) {
            return ToolExecutionResult.error("图片处理失败：" + e.getMessage());
        }
    }
    
    private BufferedImage resizeImage(BufferedImage image, int width, int height) {
        Image scaled = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(scaled, 0, 0, null);
        g2d.dispose();
        return resized;
    }
    
    private BufferedImage cropImage(BufferedImage image, int x, int y, int width, int height) {
        return image.getSubimage(x, y, Math.min(width, image.getWidth()), Math.min(height, image.getHeight()));
    }
    
    private BufferedImage rotateImage(BufferedImage image, int angle) {
        double radians = Math.toRadians(angle);
        int width = image.getWidth();
        int height = image.getHeight();
        
        // 计算旋转后的尺寸
        int newWidth = (int) Math.ceil(Math.abs(width * Math.cos(radians)) + Math.abs(height * Math.sin(radians)));
        int newHeight = (int) Math.ceil(Math.abs(height * Math.cos(radians)) + Math.abs(width * Math.sin(radians)));
        
        BufferedImage rotated = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotated.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.translate((newWidth - width) / 2, (newHeight - height) / 2);
        g2d.rotate(radians, width / 2, height / 2);
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        
        return rotated;
    }
    
    private BufferedImage addWatermark(BufferedImage image, String text) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = result.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        
        // 设置水印样式
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(new Color(255, 255, 255, 128));
        
        // 计算水印位置 (右下角)
        FontMetrics metrics = g2d.getFontMetrics();
        int x = image.getWidth() - metrics.stringWidth(text) - 10;
        int y = image.getHeight() - metrics.getHeight() - 10;
        
        g2d.drawString(text, x, y);
        g2d.dispose();
        
        return result;
    }
    
    private String buildImageInfo(BufferedImage image, File file) {
        StringBuilder sb = new StringBuilder();
        sb.append("📷 图片信息\n");
        sb.append("文件名：").append(file.getName()).append("\n");
        sb.append("尺寸：").append(image.getWidth()).append(" x ").append(image.getHeight()).append(" 像素\n");
        sb.append("文件大小：").append(formatFileSize(file.length())).append("\n");
        sb.append("格式：").append(image.getType()).append("\n");
        sb.append("色彩空间：").append(image.getColorModel().getColorSpace().getType()).append("\n");
        return sb.toString();
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
