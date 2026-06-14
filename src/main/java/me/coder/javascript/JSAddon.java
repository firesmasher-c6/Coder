package me.coder.javascript;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import me.coder.api.CoderAPI;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.lang.reflect.Field;

public class JSAddon extends JavaPlugin {

    private ScriptEngine jsEngine;

    @Override
    public void onEnable() {
        CoderAPI api = CoderAPI.getInstance();
        api.log("[JS Reflection] Initializing JavaScript environment...");

        try {
            System.setProperty("idea.io.use.nio2", "true");
            
            // Load the Mozilla Rhino factory context straight from our shaded JAR
            ClassLoader localClassLoader = this.getClass().getClassLoader();
            ScriptEngineManager manager = new ScriptEngineManager(localClassLoader);
            
            this.jsEngine = manager.getEngineByExtension("js");
            if (this.jsEngine == null) {
                this.jsEngine = manager.getEngineByName("rhino");
            }

            if (this.jsEngine == null) {
                api.logError("[JS Reflection] Critical Error: Shaded Rhino JS engine failed allocation.");
                return;
            }

            api.log("[JS Reflection] Success! JavaScript engine is online.");

            // Hijack Coder's executor 10 ticks after boot complete
            Bukkit.getScheduler().runTaskLater(this, this::swapCoderExecutor, 10L);

        } catch (Exception e) {
            api.logError("[JS Reflection] Boot routine crashed: " + e.getMessage());
        }
    }

    private void swapCoderExecutor() {
        CoderAPI api = CoderAPI.getInstance();
        try {
            Plugin coderPlugin = Bukkit.getPluginManager().getPlugin("Coder");
            if (coderPlugin == null) {
                api.logError("[JS Reflection] Cannot find active 'Coder' instance.");
                return;
            }

            PluginCommand coderCmd = Bukkit.getPluginCommand("coder");
            if (coderCmd == null) {
                coderCmd = ((JavaPlugin) coderPlugin).getCommand("coder");
            }

            if (coderCmd != null) {
                Field executorField = PluginCommand.class.getDeclaredField("executor");
                executorField.setAccessible(true);
                CommandExecutor originalExecutor = (CommandExecutor) executorField.get(coderCmd);

                Command customCommandWrapper = new Command(this.jsEngine, originalExecutor);
                coderCmd.setExecutor(customCommandWrapper);
                api.log("[JS Reflection] SUCCESS! Surgically modified Coder's executor to support .js extension.");
            } else {
                api.logError("[JS Reflection] Failed to intercept Coder's native /coder command pointer.");
            }

        } catch (Exception e) {
            api.logError("[JS Reflection] Surgical executor swap aborted: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        this.jsEngine = null;
        CoderAPI.getInstance().log("[JS Reflection] Detaching proxy command matrices cleanly.");
    }
}
