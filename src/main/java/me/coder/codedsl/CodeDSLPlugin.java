package me.coder.codedsl;

import java.io.File;
import me.coder.api.CoderAPI;
import me.coder.codedsl.commands.CodeDSLCommand;
import me.coder.codedsl.manager.ScriptManager;
import org.bukkit.plugin.java.JavaPlugin;

public class CodeDSLPlugin extends JavaPlugin {

   private static CodeDSLPlugin instance;
   private CodeDSLAddon addonInstance;

   public void onEnable() {
      instance = this;
      this.saveDefaultConfig();
      this.addonInstance = new CodeDSLAddon();
      this.addonInstance.onEnable();
      ScriptManager scriptManager = this.addonInstance.getScriptManager();
      CoderAPI api = this.addonInstance.getAPI();
      File dataFolder = this.addonInstance.getDataFolder();
      if (scriptManager != null && api != null && dataFolder != null) {
         CodeDSLCommand.register(this, scriptManager, api, dataFolder);
      } else {
         this.getLogger().severe("Could not programmatically register commands: CodeDSLAddon failed to provide vital fields during setup!");
      }

   }

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
