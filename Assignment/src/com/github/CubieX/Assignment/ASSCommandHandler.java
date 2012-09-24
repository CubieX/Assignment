package com.github.CubieX.Assignment;

import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ASSCommandHandler implements CommandExecutor
{
    private final Assignment plugin;
    private final ASSConfigHandler cHandler;
    private final Logger log;
    
    // constructor
    public ASSCommandHandler(Assignment plugin, Logger log, ASSConfigHandler cHandler)
    {
        this.plugin = plugin;
        this.cHandler = cHandler;
        this.log = log;  
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player player = null;
        if (sender instanceof Player) 
        {
            player = (Player) sender;
        }

        if(plugin.getConfig().getBoolean("debug")){log.info("onCommand");}
        if (cmd.getName().equalsIgnoreCase("ass"))
        { // If the player typed /ass then do the following... (can be run from console also)
            if (args.length == 0)
            { //no arguments, so help will be displayed
                return false;
            }
            if (args.length==1)
            {
                if (args[0].equalsIgnoreCase("version")) // argument 0 is given and correct
                {            
                    sender.sendMessage(ChatColor.GREEN + "This server is running " + Assignment.logPrefix + "version " + plugin.getDescription().getVersion());
                    return true;
                } //END version   
                if (args[0].equalsIgnoreCase("reload")) // argument 0 is given and correct
                {            
                    if(sender.hasPermission("assignment.*"))
                    {
                        cHandler.reloadConfig();
                        sender.sendMessage("[" + ChatColor.GREEN + "Info" + ChatColor.WHITE + "] " + ChatColor.YELLOW + Assignment.logPrefix + plugin.getDescription().getVersion() + " reloaded!");
                        return true;
                    }
                    else
                    {
                        sender.sendMessage(ChatColor.RED + "You du not have sufficient permission to reload Assignment plugin!");
                    }
                } //END reload
                if (args[0].equalsIgnoreCase("cleanup")) // argument 0 is given and correct
                {// Deletes all invalid assignments from DB (= Assignments where there is no sign for them any more)

                    plugin.cleanupAssignmentsInDB(sender);
                    return true;
                }
            }
            else
            {
                sender.sendMessage(ChatColor.YELLOW + "Falsche Anzahl an Parametern.");
            }                

        }         
        return false; // if false is returned, the help for the command stated in the plugin.yml will be displayed to the player
    }
}
