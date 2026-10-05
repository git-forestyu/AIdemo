package org.example.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.TestCase;
import org.example.model.TestResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class ReportBuilder {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String build(List<TestCase> allCases, List<TestResult> results) {

        StringBuilder report = new StringBuilder();
        int hallucinationCount = 0;
        for (TestResult result : results) {
            if(Objects.equals(result.getActualResponse(), "AI hallucination"))
                hallucinationCount ++;
        }



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
