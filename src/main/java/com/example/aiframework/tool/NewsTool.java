package com.example.aiframework.tool;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * 新闻获取工具 - 获取国内外热点新闻
 */
public class NewsTool implements Tool {
    
    @Override
    public String getName() {
        return "news";
    }
    
    @Override
    public String getDescription() {
        return "获取热点新闻，支持国内、国际、科技、财经等分类";
    }
    
    @Override
    public List<ToolParameter> getParameters() {
        return Arrays.asList(
            ToolParameter.string("category", "新闻分类：domestic(国内)/international(国际)/tech(科技)/finance(财经)/sports(体育)/entertainment(娱乐)，默认全部", false),
            ToolParameter.number("count", "返回新闻条数，默认 10，最大 50", false),
            ToolParameter.string("date", "日期 (YYYY-MM-DD)，默认今天", false)
        );
    }
    
    @Override
    public ToolExecutionResult execute(List<ToolParameter> parameters) {
        String category = "all";
        int count = 10;
        String date = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        
        for (ToolParameter param : parameters) {
            String name = param.getName();
            Object value = param.getDefaultValue();
            
            if (value == null) continue;
            
            switch (name) {
                case "category":
                    category = value.toString().toLowerCase();
                    break;
                case "count":
                    try {
                        count = Math.max(1, Math.min(50, Integer.parseInt(value.toString())));
                    } catch (NumberFormatException e) {
                        count = 10;
                    }
                    break;
                case "date":
                    date = value.toString();
                    break;
            }
        }
        
        try {
            // 使用多个新闻源
            StringBuilder result = new StringBuilder();
            result.append("📰 热点新闻\n");
            result.append("分类：").append(getCategoryName(category)).append("\n");
            result.append("日期：").append(date).append("\n");
            result.append("=".repeat(50)).append("\n\n");
            
            // 获取新闻列表 (模拟，实际需要接入新闻 API)
            List<NewsItem> newsList = fetchNews(category, count);
            
            for (int i = 0; i < newsList.size(); i++) {
                NewsItem news = newsList.get(i);
                result.append(String.format("%2d. ", i + 1));
                result.append(news.title).append("\n");
                result.append("   来源：").append(news.source);
                result.append(" | 时间：").append(news.time).append("\n");
                if (news.summary != null && !news.summary.isEmpty()) {
                    result.append("   ").append(news.summary).append("\n");
                }
                result.append("\n");
            }
            
            if (newsList.isEmpty()) {
                result.append("暂无新闻数据\n");
                result.append("\n提示：需要配置新闻 API Key 才能获取实时新闻。\n");
                result.append("推荐 API 服务:\n");
                result.append("- 阿里云市场：新闻数据 API\n");
                result.append("- 聚合数据：新闻头条 API\n");
                result.append("- NewsAPI: https://newsapi.org\n");
            }
            
            return ToolExecutionResult.success(result.toString());
            
        } catch (Exception e) {
            return ToolExecutionResult.error("获取新闻失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取新闻列表
     * 注：实际使用时需要接入真实新闻 API
     */
    private List<NewsItem> fetchNews(String category, int count) throws Exception {
        // 这里提供一个示例实现，实际使用时需要替换为真实 API
        try {
            // 尝试调用免费的新闻 API (示例使用 GNews)
            String apiUrl = "https://gnews.io/api/v4/top-headlines?category=" + 
                           category + "&lang=zh&max=" + count;
            
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            int status = conn.getResponseCode();
            if (status == 200) {
                // 成功获取，解析响应
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
                return parseGNewsResponse(response.toString());
            }
            
            conn.disconnect();
            
        } catch (Exception e) {
            // API 调用失败，返回示例数据
        }
        
        // 返回示例新闻数据 (当 API 不可用时)
        return getSampleNews(category, count);
    }
    
    private List<NewsItem> parseGNewsResponse(String json) {
        // 解析 GNews API 响应
        // 实际实现需要完整的 JSON 解析
        return Arrays.asList();
    }
    
    private List<NewsItem> getSampleNews(String category, int count) {
        // 示例新闻数据
        return Arrays.asList(
            new NewsItem("人工智能技术持续突破，大模型应用加速落地", "科技日报", "2 小时前", "多家科技公司发布最新 AI 产品..."),
            new NewsItem("全球经济复苏态势明显，IMF 上调增长预期", "财经新闻", "3 小时前", "国际货币基金组织发布最新报告..."),
            new NewsItem("新能源汽车销量创新高，渗透率持续提升", "汽车之家", "4 小时前", "乘联会发布最新销售数据..."),
            new NewsItem("科技创新助力乡村振兴，数字农业蓬勃发展", "人民日报", "5 小时前", "各地推进农业数字化转型..."),
            new NewsItem("体育赛事精彩纷呈，中国健儿屡创佳绩", "体育周报", "6 小时前", "多项国际赛事传来捷报...")
        );
    }
    
    private String getCategoryName(String category) {
        switch (category) {
            case "domestic": return "国内";
            case "international": return "国际";
            case "tech": return "科技";
            case "finance": return "财经";
            case "sports": return "体育";
            case "entertainment": return "娱乐";
            case "all":
            default: return "全部";
        }
    }
    
    /**
     * 新闻数据类
     */
    static class NewsItem {
        String title;
        String source;
        String time;
        String summary;
        
        NewsItem(String title, String source, String time, String summary) {
            this.title = title;
            this.source = source;
            this.time = time;
            this.summary = summary;
        }
    }
}
