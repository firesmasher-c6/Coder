package me.coder.codedsl;

import java.io.File;
import java.util.Arrays;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.coder.api.CoderAPI;
import me.coder.codedsl.commands.CodeDSLCommand;
import me.coder.codedsl.manager.ScriptManager;
import me.coder.codedsl.manager.VersionManager;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class CodeDSLPlugin extends JavaPlugin {

   private static CodeDSLPlugin instance;
   private CodeDSLAddon addonInstance;
   private VersionManager versionManager;

   @Override
   public void onEnable() {
      instance = this;
      
      // 1. Save config.yml to plugins/CodeDSL/ exclusively
      this.saveDefaultConfig();
      
      // 2. Point target directly to plugins/Coder/CodeDSL/ for addon operational resources
      File coderPluginFolder = new File(this.getDataFolder().getParentFile(), "Coder/CodeDSL");
      
      this.addonInstance = new CodeDSLAddon(coderPluginFolder, this.getDataFolder());
      this.addonInstance.onEnable();
      
      ScriptManager scriptManager = this.addonInstance.getScriptManager();
      CoderAPI api = this.addonInstance.getAPI();
      File dataFolder = this.addonInstance.getDataFolder();
      
      if (scriptManager != null && api != null && dataFolder != null) {
         // FIXED: Use pluginMeta instead of deprecated getDescription()
         String pluginVersion = this.getPluginMeta().getVersion();
         
         // Initialize VersionManager
         versionManager = new VersionManager(api, pluginVersion);
         
         // Register VersionManager as event listener for player join notifications
         this.getServer().getPluginManager().registerEvents(versionManager, this);
         
         // Check for updates asynchronously on startup
         versionManager.checkForUpdates();
         
         CodeDSLCommand executor = new CodeDSLCommand();
         // Pass versionManager parameter
         CodeDSLCommand.register(this, scriptManager, api, dataFolder, versionManager);
         
         try {
            this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
               final Commands commands = event.registrar();
               
               var commandNode = Commands.literal("codedsl")
                     .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        executor.onCommand(sender, null, "codedsl", new String[0]);
                        return 1;
                     })
                     .then(Commands.argument("args", StringArgumentType.greedyString())
                           .executes(ctx -> {
                              CommandSender sender = ctx.getSource().getSender();
                              String rawArgs = StringArgumentType.getString(ctx, "args");
                              String[] splitArgs = rawArgs.split(" ");
                              
                              executor.onCommand(sender, null, "codedsl", splitArgs);
                              return 1;
                           })
                     )
                     .build();

               commands.register(
                  commandNode,
                  "Execute and manage CodeDSL scripts",
                  Arrays.asList("cdsl", "code-dsl")
               );
            });

            if (api != null) {
               api.log("CodeDSL commands registered successfully using Paper Lifecycle API.");
               api.log("VersionManager initialized and checking for updates...");
            }
         } catch (Exception e) {
            if (api != null) {
               api.logError("Failed to register commands via Paper Lifecycle: " + e.getMessage());
            }
            e.printStackTrace();
         }
      } else {
         this.getLogger().severe("Could not programmatically register commands: CodeDSLAddon failed to provide vital fields during setup!");
      }
   }

   @Override
   public void onDisable() {
      if (this.addonInstance != null) {
         this.addonInstance.onDisable();
      }
   }

   public static CodeDSLPlugin getInstance() {
      return instance;
   }

   public CodeDSLAddon getAddonInstance() {
      return this.addonInstance;
   }

   public VersionManager getVersionManager() {
      return this.versionManager;
   }
}