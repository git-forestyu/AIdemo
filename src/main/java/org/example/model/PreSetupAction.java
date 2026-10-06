package org.example.model;

//Not handled for now. To be used later, before a delete, to fetch the methods under the same path so the create can run first
public class PreSetupAction {
    private String target;          // "POST /api/users"
    private String contentType;     // "application/json"
    private String requestBody;     // {"name":"test","age":25}
    private int expectedStatus;     // 200
}