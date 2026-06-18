package me.coder.codedsl;

import java.io.File;
import java.util.Arrays;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.coder.api.CoderAPI;
import me.coder.codedsl.commands.CodeDSLCommand;
import me.coder.codedsl.manager.ScriptManager;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class CodeDSLPlugin extends JavaPlugin {

   private static CodeDSLPlugin instance;
   private CodeDSLAddon addonInstance;

   @Override
   public void onEnable() {
      instance = this;
      this.saveDefaultConfig();
      this.addonInstance = new CodeDSLAddon();
      this.addonInstance.onEnable();
      ScriptManager scriptManager = this.addonInstance.getScriptManager();
      CoderAPI api = this.addonInstance.getAPI();
      File dataFolder = this.addonInstance.getDataFolder();
      
      if (scriptManager != null && api != null && dataFolder != null) {
         // Create command executor instance
         CodeDSLCommand executor = new CodeDSLCommand();
         
         // Initialize static fields in your command class
         CodeDSLCommand.register(this, scriptManager, api, dataFolder);
         
         // Modern Paper Command Registration using Lifecycle Events
         try {
            this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
               final Commands commands = event.registrar();
               
               // Build the command tree using a greedy string argument to consume subcommands
               var commandNode = Commands.literal("codedsl")
                     // Handles base execution: /codedsl
                     .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        executor.onCommand(sender, null, "codedsl", new String[0]);
                        return 1;
                     })
                     // Handles subcommands and additional arguments: /codedsl <subcommand> [args]
                     .then(Commands.argument("args", StringArgumentType.greedyString())
                           .executes(ctx -> {
                              CommandSender sender = ctx.getSource().getSender();
                              String rawArgs = StringArgumentType.getString(ctx, "args");
                              
                              // Split raw text into an args array to mock legacy Bukkit execution
                              String[] splitArgs = rawArgs.split(" ");
                              
                              executor.onCommand(sender, null, "codedsl", splitArgs);
                              return 1;
                           })
                     )
                     .build();

               // Register the constructed literal node, description, and aliases
               commands.register(
                  commandNode,
                  "Execute and manage CodeDSL scripts",
                  Arrays.asList("cdsl", "code-dsl")
               );
            });

            if (api != null) {
               api.log("CodeDSL commands registered successfully using Paper Lifecycle API.");
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
}