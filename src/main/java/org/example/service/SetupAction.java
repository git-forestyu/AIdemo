package org.example.service;

//暂时不处理，用作后面删除前获取同地址下的方法先进行新增
public class SetupAction {
    private String target;          // "POST /api/users"
    private String contentType;     // "application/json"
    private String requestBody;     // {"name":"test","age":25}
    private int expectedStatus;     // 200
}