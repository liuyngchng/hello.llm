package hello.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;


// create a java maven project with cmd: mvn archetype:generate
//  dd dependency in your pom file
//  JDK version > 11, JDK 17 is used and tested in this project.
//   <dependency>
//    <groupId>com.fasterxml.jackson.core</groupId>
//   <artifactId>jackson-databind</artifactId>
//   <version>2.15.0</version>
//</dependency>
public class Txt2Sql {

    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions";
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public Txt2Sql(String apiKey) {
        this.apiKey = Objects.requireNonNull(apiKey, "API key cannot be null");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 将自然语言转换为SQL语句
     * @param question 用户问题
     * @param schema 数据库schema信息
     * @return 生成的SQL语句
     * @throws Txt2SqlException 转换过程中发生错误时抛出
     */
    public String convertToSql(String question, String schema) throws Txt2SqlException {
        try {
            // 构建系统提示词
            String systemPrompt = buildSystemPrompt(schema);
            
            // 构建请求消息
            ChatRequest request = new ChatRequest(
                "deepseek-chat",
                new Message[]{
                    new Message("system", systemPrompt),
                    new Message("user", question)
                },
                0.7,
                4096
            );

            // 发送请求到DeepSeek API
            String responseJson = sendRequest(request);
            
            // 解析响应并提取SQL语句
            return extractSqlFromResponse(responseJson);
            
        } catch (Exception e) {
            throw new Txt2SqlException("Failed to convert text to SQL", e);
        }
    }

    /**
     * 构建系统提示词，包含schema信息和转换规则
     */
    private String buildSystemPrompt(String schema) {
        return String.format(
            "你是一个专业的SQL生成助手。请根据提供的数据库schema和用户问题，生成准确且高效的SQL查询语句。\n\n" +
            "数据库Schema信息:\n%s\n\n" +
            "请遵循以下规则:\n" +
            "1. 只输出SQL语句，不要包含任何解释或额外文本\n" +
            "2. 确保SQL语法正确且符合标准SQL规范\n" +
            "3. 使用合适的JOIN语句连接相关表\n" +
            "4. 包含必要的WHERE条件过滤\n" +
            "5. 如果用户问题中涉及时间范围，请使用合适的日期函数\n" +
            "6. 优先使用EXISTS而不是IN子查询以提高性能\n" +
            "7. 避免使用SELECT *，明确指定需要的字段\n\n" +
            "请根据上述schema和用户问题生成SQL语句:", 
            schema
        );
    }

    /**
     * 发送HTTP请求到DeepSeek API
     */
    private String sendRequest(ChatRequest request) throws Exception {
        String requestBody = objectMapper.writeValueAsString(request);
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(DEEPSEEK_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response = httpClient.send(
            httpRequest, 
            HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException("API request failed with status: " + 
                response.statusCode() + ", body: " + response.body());
        }

        return response.body();
    }

    /**
     * 从API响应中提取SQL语句
     */
    private String extractSqlFromResponse(String responseJson) throws Exception {
        ChatResponse response = objectMapper.readValue(responseJson, ChatResponse.class);
        
        if (response.choices == null || response.choices.length == 0) {
            throw new RuntimeException("No choices in API response");
        }
        
        Message message = response.choices[0].message;
        if (message == null || message.content == null) {
            throw new RuntimeException("No message content in API response");
        }
        
        // 清理SQL语句，移除可能的代码块标记
        String sql = message.content.trim();
        if (sql.startsWith("```sql")) {
            sql = sql.substring(6);
        }
        if (sql.startsWith("```")) {
            sql = sql.substring(3);
        }
        if (sql.endsWith("```")) {
            sql = sql.substring(0, sql.length() - 3);
        }
        
        return sql.trim();
    }

    // 请求和响应数据类
    private static class ChatRequest {
        public String model;
        public Message[] messages;
        public double temperature;
        @JsonProperty("max_tokens")
        public int maxTokens;

        public ChatRequest(String model, Message[] messages, double temperature, int maxTokens) {
            this.model = model;
            this.messages = messages;
            this.temperature = temperature;
            this.maxTokens = maxTokens;
        }
    }

    private static class Message {
        public String role;
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    private static class ChatResponse {
        public Choice[] choices;
    }

    private static class Choice {
        public Message message;
    }

    // 自定义异常类
    public static class Txt2SqlException extends Exception {
        public Txt2SqlException(String message) {
            super(message);
        }

        public Txt2SqlException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // 使用示例
    public static void main(String[] args) {
//        String apiKey = "your_deepseek_api_key_here";
        String apiKey = "sk-7575ec4f3115433ab487d380495081b2";
        Txt2Sql converter = new Txt2Sql(apiKey);
        
        // 示例数据库schema
        String schema = "表结构:\n" +
            "1. users表: id (int, 主键), name (varchar), email (varchar), created_at (datetime)\n" +
            "2. orders表: id (int, 主键), user_id (int, 外键), amount (decimal), status (varchar), order_date (datetime)\n" +
            "3. products表: id (int, 主键), name (varchar), price (decimal)\n" +
            "4. order_items表: id (int, 主键), order_id (int, 外键), product_id (int, 外键), quantity (int)";
        
        // 示例用户问题
        String question = "查询最近一个月下单金额超过1000元的用户姓名和总金额";
        
        try {
            String sql = converter.convertToSql(question, schema);
            System.out.println("生成的SQL语句:");
            System.out.println(sql);
        } catch (Txt2SqlException e) {
            System.err.println("转换失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
