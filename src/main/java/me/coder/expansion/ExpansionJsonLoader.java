package me.coder.expansion;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.Enumeration;

public class ExpansionJsonLoader {
    
    private String expansionName;
    private Map<String, JsonArray> loadedJsonFiles;
    private Map<String, String> jsonFileContent;
    
    public ExpansionJsonLoader(String expansionName) {
        this.expansionName = expansionName;
        this.loadedJsonFiles = new HashMap<>();
        this.jsonFileContent = new HashMap<>();
    }
    
    public void loadFromArchive(ZipFile zipFile) throws IOException {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.getName().startsWith("SRC/") && entry.getName().endsWith(".json")) {
                try {
                    String fileName = entry.getName().substring(4);
                    String content = readZipEntryAsString(zipFile, entry.getName());
                    
                    JsonSyntaxValidator.ValidationResult validation = 
                        JsonSyntaxValidator.validateJsonString(content);
                    
                    if (!validation.isValid) {
                        throw new IOException(validation.errorMessage);
                    }
                    
                    JsonArray jsonArray = JsonParser.parseString(content).getAsJsonArray();
                    loadedJsonFiles.put(fileName, jsonArray);
                    jsonFileContent.put(fileName, content);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to load JSON file: " + entry.getName() + " - " + e.getMessage());
                }
            }
        }
    }
    
    public Map<String, JsonArray> getLoadedJsonFiles() {
        return loadedJsonFiles;
    }
    
    public Map<String, String> getJsonFileContent() {
        return jsonFileContent;
    }
    
    public JsonArray getJsonFile(String fileName) {
        return loadedJsonFiles.get(fileName);
    }
    
    public String getJsonFileContent(String fileName) {
        return jsonFileContent.get(fileName);
    }
    
    public boolean hasJsonFile(String fileName) {
        return loadedJsonFiles.containsKey(fileName);
    }
    
    public Map<String, String> resolveImports(JsonArray jsonArray) {
        Map<String, String> resolvedImports = new HashMap<>();
        
        for (int i = 0; i < jsonArray.size(); i++) {
            var element = jsonArray.get(i);
            if (element.isJsonObject()) {
                var obj = element.getAsJsonObject();
                if (obj.has("IMPORTS")) {
                    var imports = obj.getAsJsonObject("IMPORTS");
                    for (String importKey : imports.keySet()) {
                        String importPath = imports.get(importKey).getAsString();
                        
                        if (loadedJsonFiles.containsKey(importPath + ".json")) {
                            resolvedImports.put(importKey, importPath);
                        }
                    }
                }
            }
        }
        
        return resolvedImports;
    }
    
    private String readZipEntryAsString(ZipFile zipFile, String entryName) throws IOException {
        try (InputStream input = zipFile.getInputStream(zipFile.getEntry(entryName));
             BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }
}