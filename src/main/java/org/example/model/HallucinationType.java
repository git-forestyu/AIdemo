package org.example.model;

public enum HallucinationType {
    MISSING_TARGET,        // target 为空
    MISSING_PATH,          // target 里没有 /
    INVALID_HTTP_METHOD,   // target 不以合法 HTTP 方法开头
    VAGUE_ASSERTION        // expected 含模糊词
}