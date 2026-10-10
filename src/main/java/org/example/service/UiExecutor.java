package org.example.service;

import org.example.model.HallucinationValidationResult;
import org.example.model.TestCase;
import org.example.model.TestResult;
import org.example.model.UiTestCase;
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

    public List<TestResult> executeScenario(List<UiTestCase> cases) {
        WebDriver driver = new ChromeDriver();
        List<TestResult> results = new ArrayList<>();

        try {
            // 打开页面（假设所有用例的 target 是同一个页面）
            driver.get("http://localhost:8080/login.html");

            for (UiTestCase tc : cases) {
                // 执行 action
                try {
                    if (tc.getAction().startsWith("input")) {
                        String[] parts = tc.getAction().split(" ");
                        String[] kv = parts[1].split("=");

                        String fieldId = kv[0];
                        String value = kv[1];

                        driver.findElement(By.id(fieldId)).sendKeys(value);
                        // Validation for the input
                        String actualValue = driver.findElement(By.id(fieldId)).getAttribute("value");
                        boolean passed = value.equals(actualValue);
                        results.add(new TestResult(toTestCase(tc), passed ? 200 : 0, passed,
                                "expected value: " + value + ", actual value: " + actualValue));
                        Thread.sleep(1000);
                    } else if (tc.getAction().startsWith("click")) {
                        String elementId = tc.getAction().split(" ")[1];
                        driver.findElement(By.id(elementId)).click();
                        Thread.sleep(1000);
                        results.add(new TestResult(toTestCase(tc), 200, true, "click passed"));
                    } else if (tc.getAction().startsWith("assert")) {
                        String[] parts = tc.getAction().split(" ");
                        String[] kv = parts[1].split("=");
                        Thread.sleep(1000);
                        if ("url".equals(kv[0])) {
                            boolean ok = driver.getCurrentUrl().contains(kv[1]);
                            results.add(new TestResult(toTestCase(tc), ok ? 200 : 0, ok, driver.getCurrentUrl()));
                        } else {
                            results.add(new TestResult(toTestCase(tc), 0, false, "Unknown assert target"));
                        }
                    }
                }
                catch(Exception e) {
                    log.error("Failed to execute action: {}", tc.getAction(), e);
                    results.add(new TestResult(toTestCase(tc), 0, false, e.getMessage()));
                }
            }
        }
        finally {
            driver.quit();
        }

        return results;
    }

    @Override
    public HallucinationValidationResult validateAIhallucination(TestCase tc) {
        // UI 用例暂时不做幻觉校验，直接返回 valid
        return HallucinationValidationResult.valid();
    }

    public TestCase toTestCase(UiTestCase uiTc) {
        TestCase tc = new TestCase();
        tc.setAction(uiTc.getAction());
        tc.setTarget(uiTc.getTarget());
        tc.setExpected(uiTc.getExpected());
        tc.setExpectedStatus(uiTc.getExpectedStatus());
        return tc;
    }
}
