package rd.test;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;




public class Txt2Sql {

    private static final Logger LOGGER = LogManager.getLogger();

    private final String apiUri;
    private final String apiKey;
    private final String modelName;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public Txt2Sql(String apiUri, String apiKey, String modelName) {
        this.apiUri = Objects.requireNonNull(apiUri, "API URI cannot be null");
        this.apiKey = Objects.requireNonNull(apiKey, "API key cannot be null");
        this.modelName = Objects.requireNonNull(modelName, "modelName cannot be null");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 将自然语言转换为SQL语句，直接返回原始JSON响应
     */
    public String convertToSql(String question, String schema) throws Txt2SqlException {
        try {
            // 构建系统提示词
            String systemPrompt = buildSystemPrompt(schema);

            // 构建请求消息
            ChatRequest request = new ChatRequest(
                    this.modelName,
                    new Message[]{
                            new Message("system", systemPrompt),
                            new Message("user", question)
                    },
                    0.7,
                    4096
            );

            // 发送请求到DeepSeek API并直接返回原始JSON
            return sendRequest(request);

        } catch (Exception e) {
            throw new Txt2SqlException("Failed to convert text to SQL", e);
        }
    }

    /**
     * 构建系统提示词
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
     * 发送HTTP请求到DeepSeek API，直接返回JSON字符串
     */
    private String sendRequest(ChatRequest request) throws Exception {
        String requestBody = objectMapper.writeValueAsString(request);
        LOGGER.info("start HTTP request to {}", this.apiUri);
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(this.apiUri))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + this.apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(60))
                .build();

        final HttpResponse<String> response = httpClient.send(
                httpRequest,
                HttpResponse.BodyHandlers.ofString()
        );

        LOGGER.info("HTTP response status {}, body, {}", response.statusCode(), response.body());
        return response.body();
    }

    // 请求数据类（只需要请求的，响应的不需要了）
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
        Thread.currentThread().setName("boot");
        Configurator.initialize("Log4j2", "./config/log4j2.xml");
        Map<String, Object> myCfg = SysInit.initYmlCfg();
        Map<String, Object> apiConfig = (Map<String, Object>) myCfg.get("api");
        String apiUri = String.format("%s/chat/completions", apiConfig.get("llm_api_uri"));
        String apiKey = (String) apiConfig.get("llm_api_key");
        String modelName = (String) apiConfig.get("llm_model_name");
        LOGGER.info("api_uri {}, api_key {}, model {}", apiUri, apiKey, modelName);
        Txt2Sql converter = new Txt2Sql(apiUri, apiKey, modelName);

        // 示例数据库schema
        String schema = "表结构:\n" +
                "1. users表: id (int, 主键), name (varchar), email (varchar), created_at (datetime)\n" +
                "2. orders表: id (int, 主键), user_id (int, 外键), amount (decimal), status (varchar), order_date (datetime)\n" +
                "3. products表: id (int, 主键), name (varchar), price (decimal)\n" +
                "4. order_items表: id (int, 主键), order_id (int, 外键), product_id (int, 外键), quantity (int)";

        // 示例用户问题
        String question = "查询最近一个月下单金额超过1000元的用户姓名和总金额";

        try {
            String jsonResponse = converter.convertToSql(question, schema);
            JSONObject object = JSONObject.parseObject(jsonResponse);
            LOGGER.info("convert_result: {}", object.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content"));

        } catch (Txt2SqlException e) {
            LOGGER.error("convert failed", e);
        }
    }
}
