package org.example.service;

import org.example.model.*;
import org.springframework.stereotype.Service;


@Service
public interface TestExecutor {
    HallucinationValidationResult validateAIhallucination(TestCase tc);
    TestResult executeAndPostVerification(TestCase tc);
}


