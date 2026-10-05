package org.example.service;

import com.fasterxml.jackson.annotation.JsonInclude;

public class TestCase {
    private String action;
    private String target;
    private String expected;
    private int expectedStatus;
    private String parameter;
    private String requestBody;
    private String contentType;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private SetupAction setUp;
    @JsonInclude(JsonInclude.Include.NON_NULL) //为空是不进行解析和显示
    private VerifyAfter verifyAfter;

    public VerifyAfter getVerifyAfter() { return verifyAfter; }
    public void setVerifyAfter(VerifyAfter verifyAfter) { this.verifyAfter = verifyAfter; }


    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getExpected() { return expected; }
    public void setExpected(String expected) { this.expected = expected; }
    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }
    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getExpectedStatus() {
        return expectedStatus;
    }

    public void setExpectedStatus(int expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    public SetupAction getSetUp() {
        return setUp;
    }

    public void setSetUp(SetupAction setUp) {
        this.setUp = setUp;
    }
}