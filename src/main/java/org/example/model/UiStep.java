package org.example.model;

public class UiStep {
    private String action;   // "open", "input", "click", "assert"，
    private String target;   // "username", "loginBtn", "url", "/login.html"
    private String value;    // "test", "/home.html"

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}