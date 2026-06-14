package me.coder.javafixer;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class JavaFixerPlugin extends JavaPlugin {

    private static JavaFixerPlugin instance;
    private File javaClassesFolder;
    private JavaFixerAddon addonInstance;

    @Override
    public void onEnable() {
        instance = this;
        
        // Setup clean target directory paths
        this.javaClassesFolder = new File(getDataFolder().getParentFile(), "Coder/JavaClasses");
        if (!javaClassesFolder.exists()) {
            javaClassesFolder.mkdirs();
        }

        // Create the addon entity and trigger its internal setup routines
        this.addonInstance = new JavaFixerAddon(this);
        this.addonInstance.onEnable();

        getLogger().info("CoderJavaFixer Core Plugin active.");
    }

    @Override
    public void onDisable() {
        if (this.addonInstance != null) {
            this.addonInstance.onDisable();
        }
        getLogger().info("CoderJavaFixer Core Plugin disabled.");
    }

    public File getJavaClassesFolder() {
        return javaClassesFolder;
    }

    public static JavaFixerPlugin getInstance() {
        return instance;
    }
}