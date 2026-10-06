package org.example.model;

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
    private PreSetupAction setUp;
    @JsonInclude(JsonInclude.Include.NON_NULL) //when null it is neither parsed nor displayed
    private PostVerification postVerification;


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

    public PreSetupAction getSetUp() {
        return setUp;
    }

    public void setSetUp(PreSetupAction setUp) {
        this.setUp = setUp;
    }

    public PostVerification getPostVerification() {
        return postVerification;
    }

    public void setPostVerification(PostVerification postVerification) {
        this.postVerification = postVerification;
    }
}