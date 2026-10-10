package org.example.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.HallucinationRecord;
import org.example.model.HallucinationType;
import org.example.model.TestCase;
import org.example.model.TestResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportBuilder {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String build(List<TestCase> allCases, List<TestResult> results, List<HallucinationRecord> hallucinations) {

        StringBuilder report = new StringBuilder();
        /*int hallucinationCount = 0;
        for (TestResult result : results) {
            if(Objects.equals(result.getActualResponse(), "AI hallucination"))
                hallucinationCount ++;
        }*/

        report.append("AI Hallucination Analysis:\n");

        if (hallucinations.isEmpty()) {
            report.append("  No hallucinations detected.\n\n");
        } else {
            Map<HallucinationType, Long> stats = hallucinations.stream()
                    .collect(Collectors.groupingBy(HallucinationRecord::getType, Collectors.counting()));

            for (Map.Entry<HallucinationType, Long> entry : stats.entrySet()) {
                report.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            report.append("\n");

            // 列出每条幻觉的具体原因
            report.append("Hallucination Details:\n");
            for (HallucinationRecord h : hallucinations) {
                report.append("  - [").append(h.getType()).append("] ")
                        .append(h.getReason()).append("\n");
            }
            report.append("\n");
        }



        report.append("Total generated cases: ").append(allCases.size()).append("\n");
        report.append("Executed: ").append(results.size()).append("\n");
        //report.append("AI hallucination count: ").append(hallucinationCount).append("\n");

        long passed = results.stream().filter(TestResult::isPassed).count();
        report.append("Passed: ").append(passed).append(" / ").append(results.size()).append("\n");

       // Pass Rate
        double passRate = results.isEmpty() ? 0 : (double) passed / results.size() * 100;
        report.append("Pass rate: ").append(String.format("%.1f%%", passRate)).append("\n");

        int count=1;
        for (TestResult r : results) {
            String tcJson;
            try {
                tcJson = MAPPER.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(r.getTestCase());
            } catch (JsonProcessingException e) {
                tcJson = "Serialization failed: " + e.getMessage();
            }
            report.append("#####Test Case ").append(count).append(":\n").append(tcJson).append("\n");
            report.append("Test Result: ").append(r.isPassed() ? "[Passed] " : "[Failed] ").append("\nTarget/Path: ").append(r.getTestCase().getTarget()).append("\nTest Point: ")
                    .append(r.getTestCase().getAction()).append("\nExpected Result:\n").append("a. expected status code: ").append(r.getTestCase().getExpectedStatus()).append("\n").append("b. expected response: ").append(r.getTestCase().getExpected()).append("\n").append("Actual Result:\n").append("a. actual status code: ").append(r.getStatusCode()).append("\nb. actual response: ").append(r.getActualResponse()).append("\n\n");
            count++;
        }

        return report.toString();
    }
}
