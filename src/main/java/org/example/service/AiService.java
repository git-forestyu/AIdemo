package org.example.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private final ChatClient chatClient;

    // 构造函数注入 Spring Boot 自动配置好的 ChatClient
    public AiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 向 DeepSeek 发送消息并获取回复
     * @param prompt 用户输入的问题或指令
     * @return AI 返回的文本
     */
    public String ask(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    /**
     * 带系统提示词的调用，可以约束 AI 的行为
     * 非常适合自动化平台：让 AI 扮演“测试用例生成器”或“代码分析器”
     */
    public String askWithSystem(String systemPrompt, String userPrompt) {
        return chatClient.prompt()
                .system(systemPrompt) // 设定 AI 的角色
                .user(userPrompt)      // 用户的具体指令
                .call()
                .content();
    }


    public String generateTestCasesJsonWithAIhallucination(String apiDescription) {
        String systemPrompt = """
        你是一个测试用例生成专家。
        输入是一段接口描述，输出JSON数组。
        数组每个元素包含六个字段：
        - action: 测试动作描述
        - target: 请求方法和路径
        - parameter: 传入url中的查询请求参数
        - contentType: 请求方法的content type
        - requestBody: 请求的body
        - expected: 期望结果
        - expectedStatus: 期望状态
       """;
        return askWithSystem(systemPrompt, apiDescription);
    }

    public String generateTestCasesJson(String apiDescription) {
        String systemPrompt = """
        你是一个测试用例生成专家。
        输入是一段接口描述，输出必须是严格的 JSON 数组，不要包含任何解释文字。
        数组每个元素包含六个字段：
        - action: 测试动作描述，不能为空
        - target: 请求方法和路径，方法和路径都不能为空，设计用例时，方法按照我传给你的HTTP方法，不要自行改动
        - parameter: get方法表示拼装了请求参数的路径，必须是字符串,以问号开始，如?parameter=test，post时方法为空
        - contentType: post方法请求的content type，如application/json等，直接我提供给你的接口定义里取值，通常在接口定义的requestBody.content字段，不能随便修改，接口定义没有值才能设置为空；get方法时设置为空
        - requestBody: post方法请求的body，可以为空或不为空，不为空时必须是字符串类型，且必须要严格匹配contentType的设置，比如ContentType是application/json类型且请求体是JSON对象的话，请把它序列化成字符串再放入，例如"{\"name\":\"test\"}"，不要直接输出JSON对象；get方法时设置为空
        - expected: 期望结果，文本描述，不能为空，这个字段不需要包含status code的预期取值了
        - expectedStatus: 期望状态，如400,200等，不能为空
        每条用例的 expected 必须是唯一确定的断言，不允许出现“或”“可能”“之一”这类模糊表述。如果不确定，就基于接口定义写最保守的断言。
        """;
        return askWithSystem(systemPrompt, apiDescription);
    }



    public List<TestCase> parseToTestCases(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, new TypeReference<List<TestCase>>() {});
    }

    public List<ApiEndpoint> parseSwaggerToEndpoint(String swaggerJson) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(swaggerJson);
        JsonNode paths = root.get("paths");

        List<ApiEndpoint> endpoints = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> it = paths.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            String path = entry.getKey();
            JsonNode pathItem = entry.getValue();

            //调试代码，先只测对应的方法
          // if (!path.equals("/ai/run-test"))
            if (path.contains("/ai/"))
                continue;
            if (path.startsWith("/v3/") || path.startsWith("/swagger") || path.startsWith("/actuator"))
                continue;

            // 一个路径下可能有多个方法（GET/POST）
            Iterator<Map.Entry<String, JsonNode>> methods = pathItem.fields();
            while (methods.hasNext()) {
                Map.Entry<String, JsonNode> methodEntry = methods.next();
                String method = methodEntry.getKey().toUpperCase();  // GET / POST
                JsonNode methodNode = methodEntry.getValue();

                JsonNode schemaNode = methodNode.at("/requestBody/content/application~1json/schema");
                String schemaJson = "";

                //schemaNode.has只支持application/json，如果有 application/x-www-form-urlencoded 或 multipart/form-data，schema 会拿不到
                if (schemaNode.has("$ref")) {
                    String ref = schemaNode.get("$ref").asText();   // "#/components/schemas/User"
                    String schemaName = ref.substring(ref.lastIndexOf("/") + 1);  // "User"
                    JsonNode realSchema = root.at("/components/schemas/" + schemaName);
                    schemaJson = realSchema != null ? realSchema.toString() : "";
                } else {
                    schemaJson = schemaNode.toString();
                }


                ApiEndpoint ep = new ApiEndpoint();
                ep.setPath(path);
                ep.setMethod(method);
                ep.setRawJson(methodNode.toString());  // 把这个接口的原始 JSON 留着喂给 AI
                ep.setSchemaJson(schemaJson);
                endpoints.add(ep);
            }
                    }
        return endpoints;
    }
}
