package me.coder.expansion;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class ExpansionMetadata {
    
    private String name;
    private String version;
    private String author;
    private String description;
    private String entry;
    private String type;
    private String apiVersion;
    private JsonObject rawManifest;
    
    public ExpansionMetadata(String manifestJson) throws IllegalArgumentException {
        try {
            this.rawManifest = JsonParser.parseString(manifestJson).getAsJsonObject();
            this.name = getStringOrNull("name");
            this.version = getStringOrNull("version");
            this.author = getStringOrNull("author");
            this.description = getStringOrNull("description");
            this.entry = getStringOrNull("entry");
            this.type = getStringOrNull("type");
            this.apiVersion = getStringOrNull("apiVersion");
            
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Expansion name is required");
            }
            if (version == null || version.isEmpty()) {
                throw new IllegalArgumentException("Expansion version is required");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid manifest JSON: " + e.getMessage());
        }
    }
    
    private String getStringOrNull(String key) {
        JsonElement element = rawManifest.get(key);
        if (element != null && element.isJsonPrimitive()) {
            return element.getAsString();
        }
        return null;
    }
    
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getAuthor() { return author; }
    public String getDescription() { return description; }
    public String getEntry() { return entry; }
    public String getType() { return type; }
    public String getApiVersion() { return apiVersion; }
    public JsonObject getRawManifest() { return rawManifest; }
}