
package org.example.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@RestController
public class TestController {

    @Autowired
    private AiService aiService;
    @Autowired
    private TestExecutor testExecutor;
    private static final ObjectMapper MAPPER = new ObjectMapper();

   /*
    @GetMapping("/ai/chat")
    public String chat(@RequestParam String message) {
        // 简单调用
        return aiService.ask(message);
    }

    @GetMapping("/ai/analyze")
    public String analyze(@RequestParam String code) {
        // 带系统提示词的调用，让 AI 扮演“代码审查助手”
        String systemPrompt = "你是一个代码审查专家。请分析以下 Java 代码片段，指出潜在问题并给出优化建议。";
        return aiService.askWithSystem(systemPrompt, code);
    }

    @PostMapping("/ai/generate-test")
    public String generateTest(@RequestBody String apiDescription) {
        return aiService.generateTestCases(apiDescription);
    }

    @PostMapping(value = "/ai/generatetext-test", consumes = "text/plain")
    public String generateTestText(@RequestBody String apiDescription) {
        return aiService.generateTestCases(apiDescription);
    }

    //ai通过body描述生成json，解析成test case类，java进行test case有效性校验，执行用例并生成报告
    @PostMapping("/ai/run-test")
    public String runTest(@RequestBody String apiDescription) throws JsonProcessingException {
        // 1. 调 AI 生成 JSON
        String json = aiService.generateTestCases(apiDescription);

        // 2. 解析成 List<TestCase>
        List<TestCase> cases = aiService.parseTestCases(json);
        // 3. 逐条校验
        List<TestCase> actualCases = testExecutor.validate(cases);
        // 4. 逐条执行
        List<TestResult> results = new ArrayList<>();
        for (TestCase tc : actualCases) {
            results.add(testExecutor.execute(tc));
        }
        // 5. 返回执行报告
        StringBuilder report = new StringBuilder();
        report.append("生成用例总数: ").append(cases.size()).append("\n");
        report.append("通过校验: ").append(actualCases.size()).append("\n");
        report.append("执行完成: ").append(results.size()).append("\n\n");

        long passed = results.stream().filter(TestResult::isPassed).count();
        report.append("通过: ").append(passed).append(" / ").append(results.size()).append("\n\n");

        for (TestResult r : results) {
            report.append(r.isPassed() ? "[通过] " : "[失败] ")
                    .append(r.getTestCase().getAction()).append("\n");
            if (!r.isPassed()) {
                report.append("  实际响应: ").append(r.getActualResponse()).append("\n");
            }
        }

        return report.toString();
    }

*/

    @GetMapping("/ai/run-swagger/GetTestCaseWithAIhallucination")
    public String runSwaggerWithAIhallucination() {
        String description = "接口路径: /api/users"
                + "\nHTTP方法: POST"
               +"\n传入参数：name为字符串，age为整数";


        String json = aiService.generateTestCasesJsonWithAIhallucination(description);
        return json;
    }



    //将swagger.json解析成endpoint类，endpoint组装成request body内容描述
   //通过ai将request body内容描述生成json，解析成test case类，java进行test case有效性校验，执行用例并生成报告
    StringBuilder jsonReport = new StringBuilder(); //ai生成的用例
    private static final int BATCH_SIZE = 5;
    @PostMapping(value = "/ai/run-swagger")
    public String runSwagger() throws Exception {
        // 1. 解析 Swagger，拿到所有接口
        RestClient restClient = RestClient.create();

        String swaggerJson = restClient.get()
                .uri("http://localhost:8080/v3/api-docs")
                .retrieve()
                .body(String.class);

        //防止`等markdown字符返回
        int jonsStart = swaggerJson.indexOf('{');
        int jsonEnd = swaggerJson.lastIndexOf('}');
        if (jonsStart >= 0 && jsonEnd > jonsStart) {
            swaggerJson = swaggerJson.substring(jonsStart, jsonEnd + 1);
        }


        List<ApiEndpoint> endpoints = aiService.parseSwagger(swaggerJson);

        // 2. 遍历每个接口，生成用例
        List<TestCase> allCases = new ArrayList<>();
        // 分批
        for (int i = 0; i < endpoints.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, endpoints.size());
            List<ApiEndpoint> batch = endpoints.subList(i, end);
            String description="";
            for (ApiEndpoint ep : batch) {
                // 把接口的原始 JSON + 路径和方法拼成一段描述
                 description = "接口路径: " + ep.getPath()
                        + "\nHTTP方法: " + ep.getMethod()
                        + "\n接口定义: " + ep.getRawJson()
                        + "\n请求体字段定义: " + ep.getSchemaJson();


            }
            description += "以接口路径和方法为组合维度，每个不同的组合暂时各只需要生成两条用例，一个正常用例和一个异常用例；如果是类似/api/users的GET方法，路径里不需要传任何pathvariable参数或requestparameter参数，则只需生成一条正常用例即可。\n" +
                    "因本次使用的测试场景的post方法添加用户时，id是默认设置，所以对应requestBody中不需要包含id的值";

            String json = aiService.generateTestCasesJson(description);


            //jsonReport = jsonReport.append(json).append("\n");
            List<TestCase> cases = aiService.parseToTestCases(json);
            allCases.addAll(cases);
            Thread.sleep(500);
        }
        // 3. 校验
        //List<TestCase> actualCases = testExecutor.validate(allCases);

        // 4. 执行
        List<TestResult> results = new ArrayList<>();
        TestResult result;
        int hallucinationCount = 0;
        for (TestCase tc : allCases) {
            result = testExecutor.execute(tc);
            if(Objects.equals(result.getActualResponse(), "AI hallucination"))
                hallucinationCount ++;
            results.add(result);
        }

        // 5. 出报告
        // ...（和之前一样的汇总逻辑）
        StringBuilder report = new StringBuilder();

        //把生成的测试用例打出
       // report.append("ai生成的用例: \n").append(jsonReport.toString()).append("\n\n");


        report.append("生成用例总数: ").append(allCases.size()).append("\n");
        //report.append("通过校验: ").append(actualCases.size()).append("\n");
        report.append("执行完成: ").append(results.size()).append("\n");

        report.append("AI hallucination Count数量：").append(hallucinationCount).append("\n");

        long passed = results.stream().filter(TestResult::isPassed).count();
        report.append("通过: ").append(passed).append(" / ").append(results.size()).append("\n");

        int count=1;
        for (TestResult r : results) {
            String tcJson;
            try {
                tcJson = MAPPER.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(r.getTestCase());
            } catch (JsonProcessingException e) {
                tcJson = "序列化失败: " + e.getMessage();
            }
            report.append("#####测试用例").append(count).append(":\n").append(tcJson).append("\n");
            report.append("测试结果：").append(r.isPassed() ? "[通过] " : "[失败] ").append("\n方法/路径：").append(r.getTestCase().getTarget()).append("\n测试点：")
                    .append(r.getTestCase().getAction()).append(" \n预期返回：").append("\n").append("a. expected status code:").append(r.getTestCase().getExpectedStatus()).append("\n").append("b. expected response :").append(r.getTestCase().getExpected()).append("\n").append("实际返回：").append("\n").append("a. actual status code:").append(r.getStatusCode()).append("\nb. actual response:").append(r.getActualResponse()).append("\n\n");
            count++;
        }

        return report.toString();
    }


}