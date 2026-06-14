package me.coder.javascript;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import me.coder.api.CoderAPI;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import java.io.File;
import java.io.FileReader;

public class Command implements CommandExecutor {

    private final ScriptEngine jsEngine;
    private final CommandExecutor fallbackExecutor;

    public Command(ScriptEngine jsEngine, CommandExecutor fallbackExecutor) {
        this.jsEngine = jsEngine;
        this.fallbackExecutor = fallbackExecutor;
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        // Intercept: /coder run <filename.js>
        if (args.length >= 2 && args[0].equalsIgnoreCase("run") && args[1].toLowerCase().endsWith(".js")) {
            if (!sender.hasPermission("coder.admin")) {
                sender.sendMessage("§cI'm sorry, but you do not have permission to perform this command.");
                return true;
            }
            executeJavaScriptFile(args[1], sender);
            return true;
        }

        if (this.fallbackExecutor != null) {
            return this.fallbackExecutor.onCommand(sender, command, label, args);
        }
        
        return false;
    }

    private void executeJavaScriptFile(String filename, CommandSender sender) {
        CoderAPI api = CoderAPI.getInstance();
        File scriptFile = new File("plugins/Coder/scripts/" + filename);

        if (!scriptFile.exists()) {
            sender.sendMessage("§c[Coder] Script file does not exist: " + filename);
            return;
        }

        try {
            api.log("[JS Engine] Natively executing script context for: " + filename);
            
            // Inject useful native variables straight into the JavaScript file scope
            Bindings bindings = jsEngine.createBindings();
            bindings.put("sender", sender);
            bindings.put("api", api);
            bindings.put("bukkit", org.bukkit.Bukkit.getServer());
            jsEngine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);

            FileReader reader = new FileReader(scriptFile);
            jsEngine.eval(reader);
            reader.close();
            
            sender.sendMessage("§a[Coder] JavaScript file executed successfully!");
        } catch (Exception e) {
            api.logError("[JS Engine] Runtime crash inside " + filename + ": " + e.getMessage());
            sender.sendMessage("§c[Coder] JS evaluation error. Check console.");
        }
    }
}
