package org.example.model;

public class UiTestCase {
    private String action;       // "input", "click", "assert"
    private String target;       // "username", "loginBtn", "url"
    private String value;        // "test", "/home"
    private String expected;     // "redirect to /home"
    private int expectedStatus;  // 200

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getExpected() { return expected; }
    public void setExpected(String expected) { this.expected = expected; }
    public int getExpectedStatus() { return expectedStatus; }
    public void setExpectedStatus(int expectedStatus) { this.expectedStatus = expectedStatus; }
}