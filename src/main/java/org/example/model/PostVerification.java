package org.example.model;

public class PostVerification {
    private String target;          // 验证请求，如 "GET /api/users/1"
    private int expectedStatus;     // 期望状态码，如 404
    private String response;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public int getExpectedStatus() {
        return expectedStatus;
    }

    public void setExpectedStatus(int expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    // getter / setter 省略
}
