package org.example.model;

//Not handled for now. To be used later, before a delete, to fetch the methods under the same path so the create can run first
public class PreSetupAction {
    private String target;          // "POST /api/users"
    private String contentType;     // "application/json"
    private String requestBody;     // {"name":"test","age":25}
    private int expectedStatus;     // 200

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }
    public int getExpectedStatus() { return expectedStatus; }
    public void setExpectedStatus(int expectedStatus) { this.expectedStatus = expectedStatus; }
}