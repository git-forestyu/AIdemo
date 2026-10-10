package org.example.service;

import org.example.model.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
public class UiExecutor implements TestExecutor {
    private static final Logger log = LoggerFactory.getLogger(UiExecutor.class);


    @Override
    public TestResult executeAndPostVerification(TestCase tc) {
        WebDriver driver = new ChromeDriver();
        try {
            //  target is the path of the page，action is the description of operation
            driver.get("http://localhost:8080" + tc.getTarget());

            // parse the operation from action
            String action = tc.getAction();
            if (action.startsWith("input")) {
                // "input username=test"
                String[] parts = action.split(" ");
                String[] kv = parts[1].split("=");
                driver.findElement(By.id(kv[0])).sendKeys(kv[1]);
            } else if (action.startsWith("click")) {
                String elementId = action.split(" ")[1];
                driver.findElement(By.id(elementId)).click();
            } else if (action.startsWith("assert")) {
                // "assert url=/home"
                String[] parts = action.split(" ");
                String[] kv = parts[1].split("=");
                if ("url".equals(kv[0])) {
                    boolean ok = driver.getCurrentUrl().contains(kv[1]);
                    return new TestResult(tc, ok ? 200 : 0, ok, driver.getCurrentUrl());
                }
            }

            return new TestResult(tc, 200, true, "passed");
        } catch (Exception e) {
            return new TestResult(tc, 0, false, e.getMessage());
        } finally {
            driver.quit();
        }
    }

    public List<TestResult> executeScenarios(List<UiScenario> scenarios) {
        List<TestResult> allResults = new ArrayList<>();

        for (UiScenario scenario : scenarios) {
            WebDriver driver = new ChromeDriver();
            try {
                for (UiStep step : scenario.getSteps()) {
                    TestCase tc = toTestCase(scenario, step);

                    try {
                        switch (step.getAction()) {
                            case "open":
                                driver.get("http://localhost:8080" + step.getTarget());
                                allResults.add(new TestResult(tc, 200, true, "opened " + step.getTarget()));
                                break;

                            case "input":
                                driver.findElement(By.id(step.getTarget())).sendKeys(step.getValue());
                                String actualValue = driver.findElement(By.id(step.getTarget())).getAttribute("value");
                                boolean inputOk = step.getValue().equals(actualValue);
                                allResults.add(new TestResult(tc, inputOk ? 200 : 0, inputOk,
                                        "expected: " + step.getValue() + ", actual: " + actualValue));
                                break;

                            case "click":
                                driver.findElement(By.id(step.getTarget())).click();
                                allResults.add(new TestResult(tc, 200, true, "clicked " + step.getTarget()));
                                break;

                            case "assert":
                                if (step.getTarget().endsWith(".html")) {
                                    String currentUrl = driver.getCurrentUrl();
                                    boolean ok = currentUrl.contains(step.getTarget());
                                    allResults.add(new TestResult(tc, ok ? 200 : 0, ok, currentUrl));
                                } else {
                                    allResults.add(new TestResult(tc, 0, false, "Unknown assert target: " + step.getTarget()));
                                }
                                break;

                            default:
                                allResults.add(new TestResult(tc, 0, false, "Unknown action: " + step.getAction()));
                        }
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        log.error("Step failed: {}", step.getAction(), e);
                        allResults.add(new TestResult(tc, 0, false, e.getMessage()));
                    }
                }
            } finally {
                driver.quit();
            }
        }

        return allResults;
    }

    @Override
    public HallucinationValidationResult validateAIhallucination(TestCase tc) {
        // UI 用例暂时不做幻觉校验，直接返回 valid
        return HallucinationValidationResult.valid();
    }

    private TestCase toTestCase(UiScenario scenario, UiStep step) {
        TestCase tc = new TestCase();
        tc.setAction(scenario.getName() + " | " + step.getAction());
        tc.setTarget(step.getTarget());
        tc.setExpected(step.getValue());
        tc.setExpectedStatus(200);
        return tc;
    }
}
