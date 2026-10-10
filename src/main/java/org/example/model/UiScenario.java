package org.example.model;

import java.util.List;

public class UiScenario {
    private String name;          // "Valid login"
    private List<UiStep> steps;   // 步骤数组

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<UiStep> getSteps() { return steps; }
    public void setSteps(List<UiStep> steps) { this.steps = steps; }
}