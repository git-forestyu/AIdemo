package org.example.service;

public class ApiEndpoint {
    private String path;
    private String method;
    private String rawJson;
    private String schemaJson;

    public void setPath(String path) {
        this.path = path;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

    public String getRawJson() {
        return rawJson;
    }

    public String getSchemaJson() {
        return schemaJson;
    }

    public void setSchemaJson(String schemaJson) {
        this.schemaJson = schemaJson;
    }

    // getter / setter 省略
}