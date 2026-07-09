package me.coder.expansion;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class JsonSyntaxValidator {
    
    public static class ValidationResult {
        public boolean isValid;
        public String errorMessage;
        
        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }
    }
    
    public static ValidationResult validateExpansionJson(String jsonContent) {
        try {
            JsonElement element = JsonParser.parseString(jsonContent);
            
            if (!element.isJsonArray()) {
                return new ValidationResult(false, "ERROR: Illegal Syntax Structure. Root must be a JSON array.");
            }
            
            JsonArray root = element.getAsJsonArray();
            
            for (int i = 0; i < root.size(); i++) {
                JsonElement item = root.get(i);
                if (!item.isJsonObject()) {
                    return new ValidationResult(false, "ERROR: Illegal Syntax Structure. Array items must be objects at index " + i);
                }
                
                JsonObject obj = item.getAsJsonObject();
                ValidationResult structureCheck = validateJsonStructure(obj, i);
                if (!structureCheck.isValid) {
                    return structureCheck;
                }
            }
            
            return new ValidationResult(true, null);
        } catch (JsonSyntaxException e) {
            return new ValidationResult(false, "ERROR: Illegal Syntax Structure. " + e.getMessage());
        } catch (Exception e) {
            return new ValidationResult(false, "ERROR: Illegal Syntax Structure. " + e.getMessage());
        }
    }
    
    private static ValidationResult validateJsonStructure(JsonObject obj, int index) {
        if (obj.has("IMPORTS")) {
            JsonElement imports = obj.get("IMPORTS");
            if (!imports.isJsonObject()) {
                return new ValidationResult(false, "ERROR: Illegal Syntax Structure. IMPORTS must be an object at index " + index);
            }
        }
        
        return new ValidationResult(true, null);
    }
    
    public static ValidationResult validateJsonString(String content) {
        try {
            JsonParser.parseString(content);
            return new ValidationResult(true, null);
        } catch (JsonSyntaxException e) {
            return new ValidationResult(false, "ERROR: Illegal Syntax Structure. " + e.getMessage());
        }
    }
}