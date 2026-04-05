package com.example.aiframework.tool;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 股票查询工具 - 查询 A 股/港股/美股实时行情
 */
public class StockTool implements Tool {
    
    @Override
    public String getName() {
        return "stock";
    }
    
    @Override
    public String getDescription() {
        return "查询股票实时行情，支持 A 股 (sh/sz)、港股 (hk)、美股 (gb) 代码";
    }
    
    @Override
    public List<ToolParameter> getParameters() {
        return Arrays.asList(
            ToolParameter.string("code", "股票代码，如：sh600519(茅台)、sz000001(平安银行)、hk00700(腾讯)、gbAAPL(苹果)", true),
            ToolParameter.string("type", "股票类型：a( A 股)/hk(港股)/us(美股)，默认自动识别", false)
        );
    }
    
    @Override
    public ToolExecutionResult execute(List<ToolParameter> parameters) {
        String code = null;
        String type = "auto";
        
        for (ToolParameter param : parameters) {
            String name = param.getName();
            Object value = param.getDefaultValue();
            
            if (value == null) continue;
            
            switch (name) {
                case "code":
                    code = value.toString().toLowerCase();
                    break;
                case "type":
                    type = value.toString().toLowerCase();
                    break;
            }
        }
        
        if (code == null || code.trim().isEmpty()) {
            return ToolExecutionResult.error("股票代码不能为空");
        }
        
        code = code.trim();
        
        // 自动识别市场
        if ("auto".equals(type)) {
            if (code.startsWith("sh") || code.startsWith("sz")) {
                type = "a";
            } else if (code.startsWith("hk")) {
                type = "hk";
            } else if (code.startsWith("gb") || code.matches("[A-Z]{4}")) {
                type = "us";
                if (!code.startsWith("gb")) {
                    code = "gb" + code;
                }
            } else {
                type = "a"; // 默认 A 股
                if (!code.startsWith("sh") && !code.startsWith("sz")) {
                    code = "sh" + code; // 默认上交所
                }
            }
        }
        
        try {
            StringBuilder result = new StringBuilder();
            result.append("📈 股票行情\n");
            result.append("=".repeat(50)).append("\n\n");
            
            // 获取股票数据
            StockData stock = fetchStockData(code, type);
            
            if (stock == null) {
                result.append("未找到股票数据，请检查股票代码是否正确\n");
                result.append("\n示例:\n");
                result.append("- A 股：sh600519 或 sz000001\n");
                result.append("- 港股：hk00700\n");
                result.append("- 美股：gbAAPL 或 AAPL\n");
            } else {
                result.append(formatStockData(stock));
            }
            
            return ToolExecutionResult.success(result.toString());
            
        } catch (Exception e) {
            return ToolExecutionResult.error("查询股票失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取股票数据
     */
    private StockData fetchStockData(String code, String type) throws Exception {
        try {
            // 使用新浪财经 API (免费)
            String symbol = code.replace("sh", "s_sh").replace("sz", "s_sz")
                               .replace("hk", "hk_").replace("gb", "gb_");
            
            String apiUrl = "https://hq.sinajs.cn/list=" + symbol;
            
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "StockTool/1.0");
            conn.setRequestProperty("Referer", "https://finance.sina.com.cn/");
            
            int status = conn.getResponseCode();
            if (status != 200) {
                return null;
            }
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), Charset.forName("GBK")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            conn.disconnect();
            
            return parseSinaResponse(response.toString(), code);
            
        } catch (Exception e) {
            // API 失败返回 null
            return null;
        }
    }
    
    /**
     * 解析新浪股票数据
     */
    private StockData parseSinaResponse(String response, String code) {
        try {
            // 响应格式：var hq_str_sh600519="贵州茅台，1680.00,..."
            int startIdx = response.indexOf("\"");
            int endIdx = response.lastIndexOf("\"");
            
            if (startIdx == -1 || endIdx == -1 || startIdx == endIdx) {
                return null;
            }
            
            String data = response.substring(startIdx + 1, endIdx);
            String[] fields = data.split(",");
            
            if (fields.length < 32) {
                return null;
            }
            
            StockData stock = new StockData();
            stock.name = fields[0];
            stock.code = code.toUpperCase();
            stock.open = Double.parseDouble(fields[1]);
            stock.lastClose = Double.parseDouble(fields[2]);
            stock.current = Double.parseDouble(fields[3]);
            stock.high = Double.parseDouble(fields[4]);
            stock.low = Double.parseDouble(fields[5]);
            stock.volume = Long.parseLong(fields[8]);
            stock.amount = Double.parseDouble(fields[9]);
            
            // 计算涨跌幅
            stock.change = stock.current - stock.lastClose;
            stock.changePercent = (stock.change / stock.lastClose) * 100;
            
            // 时间
            stock.date = fields[30];
            stock.time = fields[31];
            
            return stock;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 格式化股票数据输出
     */
    private String formatStockData(StockData stock) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("股票名称：").append(stock.name).append("\n");
        sb.append("股票代码：").append(stock.code).append("\n\n");
        
        sb.append("当前价格：").append(String.format("%.2f", stock.current)).append(" 元\n");
        
        // 涨跌颜色
        String changeSign = stock.change >= 0 ? "+" : "";
        String arrow = stock.change >= 0 ? "📈" : "📉";
        sb.append("涨跌额：").append(arrow).append(changeSign)
          .append(String.format("%.2f", stock.change)).append(" 元\n");
        sb.append("涨跌幅：").append(changeSign).append(String.format("%.2f", stock.changePercent)).append("%\n\n");
        
        sb.append("今开：").append(String.format("%.2f", stock.open)).append(" 元\n");
        sb.append("昨收：").append(String.format("%.2f", stock.lastClose)).append(" 元\n");
        sb.append("最高：").append(String.format("%.2f", stock.high)).append(" 元\n");
        sb.append("最低：").append(String.format("%.2f", stock.low)).append(" 元\n\n");
        
        sb.append("成交量：").append(formatNumber(stock.volume)).append(" 股\n");
        sb.append("成交额：").append(String.format("%.2f 亿", stock.amount / 100000000)).append(" 元\n\n");
        
        sb.append("数据时间：").append(stock.date).append(" ").append(stock.time);
        
        return sb.toString();
    }
    
    private String formatNumber(long num) {
        if (num >= 100000000) {
            return String.format("%.2f 亿", num / 100000000.0);
        } else if (num >= 10000) {
            return String.format("%.2f 万", num / 10000.0);
        } else {
            return String.valueOf(num);
        }
    }
    
    /**
     * 股票数据类
     */
    static class StockData {
        String name;
        String code;
        double open;
        double lastClose;
        double current;
        double high;
        double low;
        double change;
        double changePercent;
        long volume;
        double amount;
        String date;
        String time;
    }
}
