
package org.example.controller;

import org.example.model.ApiEndpoint;
import org.example.model.TestCase;
import org.example.model.TestResult;
import org.example.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;


@RestController
public class TestController {

    @Autowired
    private AiService aiService;
    @Autowired
    private TestExecutor testExecutor;
    @Autowired
    private ReportBuilder reportBuilder;

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
        return aiService.generateTestCasesJson(apiDescription);
    }

    @PostMapping(value = "/ai/generatetext-test", consumes = "text/plain")
    public String generateTestText(@RequestBody String apiDescription) {
        return aiService.generateTestCasesJson(apiDescription);
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


        List<ApiEndpoint> endpoints = aiService.parseSwaggerToEndpoint(swaggerJson);

        // 2. 遍历每个接口，生成用例
        List<TestCase> allCases = new ArrayList<>();
        // 分批
        for (int i = 0; i < endpoints.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, endpoints.size());
            List<ApiEndpoint> batch = endpoints.subList(i, end);
            StringBuilder description = new StringBuilder();
            for (ApiEndpoint ep : batch) {
                // 把接口的原始 JSON + 路径和方法拼成一段描述
                description.append("接口路径: ").append(ep.getPath())
                        .append("\nHTTP方法: ").append(ep.getMethod())
                        .append("\n接口定义: ").append(ep.getRawJson())
                        .append("\n请求体字段定义: ").append(ep.getSchemaJson())
                        .append("\n\n");


            }
            description.append("以接口路径和方法为维度，分别生成两条用例，例如接口/test/有POST和GET方法的话，POST和GET都要生成两个用例，并且都包含一个正常用例和一个异常用例；如果是类似/api/users的GET方法，路径里不需要传任何pathvariable参数或requestparameter参数，则只需生成一条正常用例即可。\n")
                    .append("因本次使用的测试场景的post方法添加用户时，id是默认设置，所以对应requestBody中不需要包含id的值");

            //暂未ai加入超时判断
            String json = aiService.generateTestCasesJson(String.valueOf(description));


            //jsonReport = jsonReport.append(json).append("\n");
            List<TestCase> cases = aiService.parseToTestCases(json);
            allCases.addAll(cases);
            Thread.sleep(500);
        }
        // 3. 校验
        ////是否生成AI hallucination的情况


        // 4. 执行
        Boolean validatedTc;
        List<TestResult> results = new ArrayList<>();
        TestResult result;

        for (TestCase tc : allCases) {
            validatedTc = testExecutor.validateAIhallucination(tc);
            if(!validatedTc)
            {
                results.add(new TestResult(tc, 0, false, "AI hallucination"));
            } else{
                result = testExecutor.executeAndPostVerificcation(tc);
                results.add(result);
            }
        }

        // 5. 出报告

        return reportBuilder.build(allCases, results);


    }


}