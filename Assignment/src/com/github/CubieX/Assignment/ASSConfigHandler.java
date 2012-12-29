package com.github.CubieX.Assignment;

import org.bukkit.configuration.file.FileConfiguration;

public class ASSConfigHandler 
{
   private FileConfiguration config;
   private final Assignment plugin;

   public ASSConfigHandler(Assignment plugin) 
   {
      this.plugin = plugin;
      config = plugin.getConfig();

      initConfig(); 
   }

   private void initConfig()
   {
      plugin.saveDefaultConfig(); //creates a copy of the provided config.yml in the plugins data folder, if it does not exist
      config = plugin.getConfig(); //re-reads config out of memory. (Reads the config from file only, when invoked the first time!)
   }

   private void saveConfig() //saves the config to disc (needed when entries have been altered via the plugin in-game)
   {
      // get and set values here!
      plugin.saveConfig();   
   }

   // reload config from disk (needed if user modified the config file manually out of game)
   public void reloadConfig()
   {
      plugin.reloadConfig();
      config = plugin.getConfig();  
      plugin.setStaticValues();
   }
}
