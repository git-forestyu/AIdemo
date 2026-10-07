package org.example.model;

public class HallucinationRecord {
    private TestCase testCase;
    private HallucinationType type;
    private String reason;

    public HallucinationRecord(TestCase testCase, HallucinationType type, String reason) {
        this.testCase = testCase;
        this.type = type;
        this.reason = reason;
    }

    public TestCase getTestCase() { return testCase; }
    public HallucinationType getType() { return type; }
    public String getReason() { return reason; }
}