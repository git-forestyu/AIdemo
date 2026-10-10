package org.example.controller;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class LoginController {

    @PostMapping("/api/login")
    public Map<String, Object> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        Map<String, Object> response = new HashMap<>();

        if ("test".equals(username) && "123456".equals(password)) {
            response.put("success", true);
            response.put("message", "Login successful");
        } else {
            response.put("success", false);
            response.put("message", "Invalid username or password");
        }

        return response;
    }
}