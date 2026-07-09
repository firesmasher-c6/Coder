package me.coder.expansion;

import com.google.gson.JsonArray;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ExpansionContext {
    
    private final String expansionName;
    private final ExpansionMetadata metadata;
    private final ExpansionJsonLoader jsonLoader;
    private final ExpansionLogger logger;
    private final ChatColorProvider colorProvider;
    private Map<String, Object> contextData;
    
    public ExpansionContext(String expansionName, ExpansionMetadata metadata, 
                          ExpansionJsonLoader jsonLoader, ExpansionLogger logger) {
        this.expansionName = expansionName;
        this.metadata = metadata;
        this.jsonLoader = jsonLoader;
        this.logger = logger;
        this.colorProvider = new ChatColorProvider();
        this.contextData = new HashMap<>();
    }
    
    public String getExpansionName() {
        return expansionName;
    }
    
    public ExpansionMetadata getMetadata() {
        return metadata;
    }
    
    public ExpansionJsonLoader getJsonLoader() {
        return jsonLoader;
    }
    
    public ExpansionLogger getLogger() {
        return logger;
    }
    
    public ChatColorProvider getColorProvider() {
        return colorProvider;
    }
    
    public void setContextData(String key, Object value) {
        contextData.put(key, value);
    }
    
    public Object getContextData(String key) {
        return contextData.get(key);
    }
    
    public boolean hasContextData(String key) {
        return contextData.containsKey(key);
    }
    
    public void removeContextData(String key) {
        contextData.remove(key);
    }
    
    public void clearContextData() {
        contextData.clear();
    }
    
    public Map<String, Object> getAllContextData() {
        return new HashMap<>(contextData);
    }
    
    public JsonArray getJsonFile(String fileName) {
        return jsonLoader.getJsonFile(fileName);
    }
    
    public String getJsonFileContent(String fileName) {
        return jsonLoader.getJsonFileContent(fileName);
    }
    
    public boolean hasJsonFile(String fileName) {
        return jsonLoader.hasJsonFile(fileName);
    }
    
    public String applyColors(String text) {
        return colorProvider.applyColors(text);
    }
    
    public String removeColors(String text) {
        return colorProvider.removeColors(text);
    }
}