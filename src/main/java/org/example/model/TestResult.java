package org.example.model;

public class TestResult {
    private TestCase testCase;
    private int statusCode;
    private boolean passed;
    private String actualResponse;

    public TestResult(TestCase testCase, int statusCode, boolean passed, String actualResponse) {
        this.testCase = testCase;
        this.statusCode = statusCode;
        this.passed = passed;
        this.actualResponse = actualResponse;
    }

    public boolean isPassed() {          // ← 这个方法必须存在
        return passed;
    }

    public TestCase getTestCase() {      // ← 这个方法也必须存在
        return testCase;
    }

    public void setActualResponse(String actualResponse) {
        this.actualResponse = actualResponse;
    }
    public String getActualResponse() {  // ← 这个方法也必须存在
        return actualResponse;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
}