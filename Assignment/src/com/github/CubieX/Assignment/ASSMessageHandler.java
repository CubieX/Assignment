package com.github.CubieX.Assignment;

/*
 * This module is for multilanguage handling.
 * Only the properties-file has to be translated.
 * */
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class ASSMessageHandler
{
   private final Assignment plugin;
   private final Logger log;
   private Locale currentLocale;
   private ResourceBundle messages;
   private MessageFormat formatter;

   // Constructor
   public ASSMessageHandler(Assignment plugin, Logger log)
   {
      this.plugin = plugin;
      this.log = log;

      ReloadLocale();
   }

   // used on creation of class and after a config reload
   public void ReloadLocale()
   {
      formatter = new MessageFormat("");
      currentLocale = new Locale(plugin.getConfig().get("language").toString());      
      formatter.setLocale(currentLocale);
      messages = ResourceBundle.getBundle("messages", currentLocale);
   }

   // Returns the value of the given key (only for values without variables/arguments!)
   public String GetMsg(String msgKey)
   {
      return (messages.getString(msgKey));
   }

   // Returns the selected message with the filled in arguments provided in msgArgs
   public String GetMsg(String msgKey, Object[] msgArgs)
   {
      formatter.applyPattern(messages.getString(msgKey));        
      String msg = formatter.format(msgArgs);

      return (msg);
   }


}
