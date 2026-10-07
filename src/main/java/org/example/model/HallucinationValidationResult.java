package org.example.model;

public class HallucinationValidationResult {
    private boolean valid;
    private HallucinationType type;
    private String reason;

    public static HallucinationValidationResult valid() {
        HallucinationValidationResult r = new HallucinationValidationResult();
        r.valid = true;
        return r;
    }

    public static HallucinationValidationResult invalid(HallucinationType type, String reason) {
        HallucinationValidationResult r = new HallucinationValidationResult();
        r.valid = false;
        r.type = type;
        r.reason = reason;
        return r;
    }

    public boolean isValid() { return valid; }
    public HallucinationType getType() { return type; }
    public String getReason() { return reason; }
}