package com.github.CubieX.Assignment;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public final class ASSEntityListener implements Listener
{
    private final Assignment plugin;
    private final Logger log;
    
    private Player assignee = null;
    private Player assigner = null;
    private String currency = "";
    private String assignmentStateActive = "active";
    
    public ASSEntityListener(Assignment plugin, Logger log)
    {
        this.plugin = plugin;
        this.log = log; 
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.currency = plugin.getConfig().getString("currency");
    }
    
    //================================================================================================
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) //For the Assigner. Fires AFTER Sign Creation
    {      
        if(plugin.getConfig().getBoolean("debug"))
        {
            log.info("im onSignChangeEvent");
            event.getPlayer().sendMessage("Linie 0:" + event.getLine(0)); //debug
        }   

        if(event.getLine(0).contains("<A>") || event.getLine(0).contains("<a>") || event.getLine(0).contains("<" + Assignment.openAssignmentTitle + ">")) //Assignment sign?
        {   
            if(plugin.getConfig().getBoolean("debug")){log.info("Assignment-Sign erkannt");}

            if(event.getPlayer().hasPermission("assignment.make") || event.getPlayer().hasPermission("assignment.*"))
            {
                assigner = event.getPlayer(); 
                String[] lineArray;
                int itemID = 0;
                byte subID = -1; //byte in JAVA has -128 to 127 range!
                int reward = 0;         
                int amount = 0;           
                boolean parsingOK = true;

                try //correct format?
                {
                    if(plugin.getConfig().getBoolean("debug")){log.info("Ist das Sign korrekt geschrieben?");}

                    lineArray = (event.getLine(1).split(":"));   //parse itemID:[subID]:amount
                    reward = Integer.parseInt(event.getLine(2)); //parse reward                
                    if(plugin.getConfig().getBoolean("debug")){log.info("ArrayLength: " + String.valueOf(lineArray.length));}

                    if(lineArray.length == 2) // no subID given
                    {
                        itemID = Byte.parseByte(lineArray[0]); //parse itemID
                        amount = Integer.parseInt(lineArray[1]); //parse amount  

                        if(!(itemID > 0 && amount > 0 && reward > 0))
                        {                        
                            parsingOK = false;
                        }
                    }
                    else
                    {
                        itemID = Integer.parseInt(lineArray[0]); //parse itemID
                        subID = Byte.parseByte(lineArray[1]); //parse subID. (in an ItemStack, subID is the "damage" argument and stored as Short)
                        amount = Integer.parseInt(lineArray[2]); //parse amount  

                        if(!(itemID > 0 && subID >= 0 && amount > 0 && reward > 0))
                        {                        
                            parsingOK = false;
                        }
                    }                    
                }
                catch (Exception e)
                {                   
                    //not a number. Abort.                
                    event.getBlock().breakNaturally();
                    assigner.sendMessage(ChatColor.YELLOW + "Hilfe: 1. Zeile: <A> 2. Zeile: ItemID:[SubID:]Anzahl 3. Zeile: Geld-Belohnung");
                    assigner.sendMessage(ChatColor.YELLOW + "Die ItemID kann mit /iteminfo abgerufen werden, wenn du das gewollte Item in der Hand hast.");
                    parsingOK = false;
                    return; //leave method
                }//TODO Evt. ItemName auf Schild anstatt der ID ermöglichen (ähnlich ChestShop). Wird aber problematisch wg. Länge...
                if(parsingOK)
                {           
                    event.setLine(0, "<" + Assignment.openAssignmentTitle + ">");
                    event.setLine(2,String.valueOf(reward).concat(" " + currency));

                    if(Assignment.econ.has(assigner.getName(), reward))  //assigner has sufficient money to set up this assignment
                    {
                        EconomyResponse ecoRes = Assignment.econ.withdrawPlayer(assigner.getName(), reward); //deposit reward in database
                        if(ecoRes.transactionSuccess()) //assigner had sufficient money to pay the reward
                        {       
                            if(assigner.getName().length() > 15)
                            {
                                event.setLine(3, assigner.getName().substring(0, 14)); //trim name to 15 characters (whole name is stored in DB, so it doesn't matter)    
                            }
                            else
                            {
                                event.setLine(3, assigner.getName()); //trim name to 15 characters (whole name is stored in DB, so it doesn't matter)
                            }

                            if(plugin.getConfig().getBoolean("debug")){log.info("Eco transfer erfolgreich");}
                            //register sign here to protect it and know if its legal or fake
                            String query = "INSERT INTO signs (assigner, x, y, z, world, reward, state) VALUES ('" + assigner.getName() + "', " + (int)event.getBlock().getX() +  ", " + (int)event.getBlock().getY()+ ", " + (int)event.getBlock().getZ() + ", '" + event.getPlayer().getWorld().getName() + "', " + reward + ", '" + assignmentStateActive + "')";                   
                            plugin.manageSQLite.insertQuery(query);
                            if(plugin.getConfig().getBoolean("debug")){log.info("SQL Query: " + query);} 
                            assigner.sendMessage(ChatColor.GREEN + "Auftrag erfolgreich erstellt!");
                        }    
                        else
                        {
                            assigner.sendMessage(ChatColor.RED + "Fehler bem uebertragen der Belohnung in die Datenbank. Bitte melde das einem Admin!");
                            log.severe(Assignment.logPrefix + "Error on depositing the reward into the DB.");
                        }                      
                    }
                    else
                    {
                        event.getBlock().breakNaturally();
                        assigner.sendMessage(ChatColor.YELLOW + "Du hast leider nicht genuegend Geld um dir diesen Auftrag leisten zu koennen.");                        
                    }                              
                }
                else
                {
                    //not a number. Abort.                
                    event.getBlock().breakNaturally();
                    assigner.sendMessage(ChatColor.YELLOW + "Hilfe: 1. Zeile: <A> 2. Zeile: ItemID:[SubID:]Anzahl 3. Zeile: Geld-Belohnung");              
                }
            }
            else
            {
                event.getBlock().breakNaturally();
                event.getPlayer().sendMessage(ChatColor.RED + "Du hast keine Berechtigung um Auftraege auszuschreiben.");
            }            
        }                
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) //For the Assignee
    {
        Action act;
        Sign sign = null; 
        String[] lineArray;
        int itemID = 0;
        short subID = 0;
        ItemStack iStack;
        int amount = 0;
        int reward = 0;
        act = event.getAction();  

        if(plugin.getConfig().getBoolean("debug")){log.info("Es klickte: " + event.getPlayer().getName());}        
        if(plugin.getConfig().getBoolean("debug")){log.info("onPlayerInteractEntity");}

        if(act == Action.RIGHT_CLICK_BLOCK)
        {
            if(plugin.getConfig().getBoolean("debug")){log.info("Rechtsklick auf BlockID " + String.valueOf(event.getClickedBlock().getTypeId())  + " erkannt");}
            if(event.getClickedBlock().getTypeId() == 63 ||
                    event.getClickedBlock().getTypeId() == 68) // Right clicked a sign on a sign on block (68) oder a signpost (63)?
            {
                if(plugin.getConfig().getBoolean("debug")){log.info("Sign erkannt");}
                sign = (Sign) event.getClickedBlock().getState();

                if(sign.getLine(0).contains("<A>") || sign.getLine(0).contains("<a>") || sign.getLine(0).contains("<" + Assignment.completedAssTag + ">") || sign.getLine(0).contains("<" + Assignment.openAssignmentTitle + ">")) // is Assignment sign?
                {
                    // block next occurring BlockPlace()-action to prevent accidental placing of block in hand                                        
                    if(event.getPlayer().getItemInHand().getTypeId() < 256) //item in hand is a placable block and not an item
                    {
                        plugin.blockNextBlockPlacing = true;
                    }

                    String assName = plugin.getAssignerNameFromDB((int)sign.getX(), (int)sign.getY(), (int)sign.getZ(), sign.getWorld().getName());
                    if(plugin.getConfig().getBoolean("debug")){log.info("assignerName: " + assName.toString() + " Spieler der klickte: " + event.getPlayer().getName());}
                    if("" != assName) //sign is registered assignment sign
                    {      
                        if(plugin.getConfig().getBoolean("debug")){log.info("Assignment-Sign erkannt");} 
                        try
                        {
                            if(assName.equalsIgnoreCase(event.getPlayer().getName().toString())) //assigner has clicked right on his own sign to pick up its remaining items from a completed assignment
                            {
                                assigner = plugin.getAssignerFromDB((int)sign.getX(), (int)sign.getY(), (int)sign.getZ(), sign.getWorld().getName());

                                if(null != assigner) // clicking player is assigner
                                {
                                    if(plugin.getConfig().getBoolean("debug")){log.info("Klickender Spieler ist Assigner (Stringvergleich)");} 
                                    if(sign.getLine(0).contains("<" + Assignment.completedAssTag + ">"))
                                    {

                                        if(plugin.getConfig().getBoolean("debug")){log.info("Abgeschlossenes AssignmentSign von Assigner gerechtsklickt");}  

                                        lineArray = sign.getLine(1).split(":");                                     
                                        if(lineArray.length == 2) //no subID given
                                        {
                                            if(plugin.getConfig().getBoolean("debug")){log.info("Zeile 1: " + lineArray[0] + " | " + lineArray[1]);}
                                            itemID = Integer.parseInt(lineArray[0]);
                                            amount = Integer.parseInt(lineArray[1]); //amount of Items the assigner can still pick up from its completed assignment
                                            iStack = new ItemStack(Material.getMaterial(itemID), amount);
                                        }
                                        else
                                        {
                                            if(plugin.getConfig().getBoolean("debug")){log.info("Zeile 1: " + lineArray[0] + " | " + lineArray[1] + " | " + lineArray[2]);}
                                            itemID = Integer.parseInt(lineArray[0]);
                                            subID = Short.parseShort(lineArray[1]);
                                            amount = Integer.parseInt(lineArray[2]); //amount of Items the assigner can still pick up from its completed assignment
                                            iStack = new ItemStack(Material.getMaterial(itemID), amount, subID);                                    
                                        }

                                        //try to add the remaining items from the assignment to the assigners inventory                                     
                                        HashMap<Integer, ItemStack> couldnotAddItems = assigner.getInventory().addItem(iStack);
                                        assigner.updateInventory(); //deprecated     

                                        if(false == couldnotAddItems.isEmpty()) //not all items fitted in the assigners inventory
                                        {
                                            int notFitting = couldnotAddItems.get(0).getAmount(); 
                                            if(subID > 0) //subID given
                                            {
                                                String stillAvailable = String.valueOf(itemID) + ":" + String.valueOf(subID) + ":" + String.valueOf(notFitting);
                                                //könnte man auch zusätzlich per Kommando erlauben. /ass abholen / collect oder so....                                        
                                                sign.setLine(1, stillAvailable);
                                            }
                                            else 
                                            {
                                                String stillAvailable = String.valueOf(itemID) + ":" + String.valueOf(notFitting);
                                                //könnte man auch zusätzlich per Kommando erlauben. /ass abholen / collect oder so....                                        
                                                sign.setLine(1, stillAvailable);  
                                            }
                                            sign.setLine(0, "<" + Assignment.completedAssTag + ">");
                                            sign.setLine(2, Assignment.rightClickText); //max. 15 Zeichen!
                                            sign.update();

                                            //keep Name of Assigner in Line 4                                           

                                            if(amount == notFitting) //Players Inventory is full
                                            {
                                                assigner.sendMessage(ChatColor.YELLOW + "Dein Inventar ist voll! Lege etwas ab, um weitere Waren deines Auftrags einzusammeln.");
                                            }
                                            else if(subID > 0) //subID given
                                            {                                  
                                                assigner.sendMessage(ChatColor.GREEN + "Deinem Inventar wurden " + ChatColor.YELLOW + (amount-notFitting) + " " + iStack.getType().getMaterial(itemID).toString() + ":" + String.valueOf(subID) + ChatColor.YELLOW + " hinzugefuegt.");
                                                assigner.sendMessage(ChatColor.GREEN + "Du kannst weitere " + notFitting + " " + iStack.getType().getMaterial(itemID).toString() + ":" + String.valueOf(subID) + " bei deinem Schild abholen.");
                                                assigner.sendMessage(ChatColor.GREEN + " (Rechtsklick)");
                                                assigner.sendMessage(ChatColor.GREEN + "Position des Schilds: X: " + String.valueOf((int)event.getClickedBlock().getX()) + "  Z: " + String.valueOf((int)event.getClickedBlock().getZ()) + " in " + event.getClickedBlock().getWorld().getName());
                                            }
                                            else
                                            {                                  
                                                assigner.sendMessage(ChatColor.GREEN + "Deinem Inventar wurden " + ChatColor.YELLOW + (amount-notFitting) + " " + iStack.getType().getMaterial(itemID).toString() + ChatColor.GREEN + " hinzugefuegt.");
                                                assigner.sendMessage(ChatColor.GREEN + "Du kannst weitere " + notFitting + " " + iStack.getType().getMaterial(itemID).toString() + " bei deinem Schild abholen.");
                                                assigner.sendMessage(ChatColor.GREEN + " (Rechtsklick)");
                                                assigner.sendMessage(ChatColor.GREEN + "Position des Schilds: X: " + String.valueOf((int)event.getClickedBlock().getX()) + "  Z: " + String.valueOf((int)event.getClickedBlock().getZ()) + " in " + event.getClickedBlock().getWorld().getName());
                                            }
                                        }
                                        else
                                        {
                                            assigner.sendMessage(ChatColor.GREEN + "Deinem Inventar wurden " + ChatColor.YELLOW + amount + " " + iStack.getType().getMaterial(itemID).toString() + ":" + String.valueOf(subID) + ChatColor.GREEN +" hinzugefuegt.");
                                            event.getClickedBlock().setType(Material.AIR); //remove sign
                                            //TODO gatheredAllGoods in Lang file. Muss noch angepasst werden wg. Compound...Variablen + chatfarben... Die oberen auch alle!
                                            plugin.deleteSignFromDB(event.getClickedBlock().getX(), event.getClickedBlock().getY(), event.getClickedBlock().getZ(), event.getClickedBlock().getWorld().getName()); //remove DB entry of sign
                                            //Assignment is now fulfilled successfully    
                                        }                                    
                                    }
                                    else // player who clicked is the assigner, but Assignment it's not yet completed. So it's nothing to pick up for him
                                    {
                                        assigner.sendMessage(ChatColor.YELLOW + "Dieser Auftrag wurde noch nicht abgeschlossen. Du kannst ihn abreissen wenn du willst.");
                                    }
                                }
                                else
                                {
                                    event.getPlayer().sendMessage(ChatColor.RED + "Fehler beim lesen des Assigners aus der DB. Bitte melde das einem Admin!");
                                    log.severe(Assignment.logPrefix + "Error on searching the assigner in DB by name.");
                                }
                            } 
                            else //clicking Player is an assignee
                            {
                                if(event.getPlayer().hasPermission("assignment.take") || event.getPlayer().hasPermission("assignment.*"))
                                {
                                    if(!sign.getLine(0).contains("<" + Assignment.completedAssTag + ">")) //assignee is about to complete the assignment
                                    {
                                        assignee = event.getPlayer();

                                        lineArray = sign.getLine(1).split(":"); //Format: amount:reward + ggf. $-Zeichen das noch weg muss!

                                        if(lineArray.length == 2) //no subID given
                                        {
                                            itemID = Integer.parseInt(lineArray[0]);                                        
                                            amount = Integer.parseInt(lineArray[1]);
                                            String temp = sign.getLine(2).replace(currency, "").trim();
                                            reward = Integer.parseInt(temp);

                                            iStack = new ItemStack(Material.getMaterial(itemID), amount);
                                            if(plugin.getConfig().getBoolean("debug")){log.info("Material: " + iStack.getType().toString() + " Anzahl: " + iStack.getAmount());}                                        
                                        }
                                        else
                                        {
                                            itemID = Integer.parseInt(lineArray[0]);
                                            subID = Short.parseShort(lineArray[1]);
                                            amount = Integer.parseInt(lineArray[2]);
                                            String temp = sign.getLine(2).replace(currency, "").trim();
                                            reward = Integer.parseInt(temp);  

                                            iStack = new ItemStack(Material.getMaterial(itemID), amount, subID);
                                            if(plugin.getConfig().getBoolean("debug")){log.info("Material: " + iStack.getType().toString() + " Anzahl: " + iStack.getAmount());}      
                                        }                                   

                                        if(plugin.getConfig().getBoolean("debug")){log.info("ItemID: " + String.valueOf(itemID) + " SubID: " + String.valueOf(subID) + ", Anzahl: " + String.valueOf(amount) + " , Belohnung: " + String.valueOf(reward));}

                                        // block next occurring BlockPlace()-action to prevent dupe bug (assignee will otherwise place the block in hand in front of the sign, causing a dupe exploit,
                                        // if it is the same block which is requested by the assignment)
                                        if(event.getPlayer().getItemInHand().getTypeId() == iStack.getTypeId())
                                        {
                                            plugin.blockNextBlockPlacing = true;
                                        }

                                        // look if assignee has needed amount of items in his inventory. If not: abort
                                        int missingAmount = 0;
                                        assignee.updateInventory(); //deprecated
                                        HashMap<Integer, ItemStack> couldNotRemove = assignee.getInventory().removeItem(iStack); //try to remove needed items
                                        assignee.updateInventory(); //deprecated

                                        if(false == couldNotRemove.isEmpty()) //there were some items missing to complete the assignment
                                        {
                                            missingAmount = couldNotRemove.get(0).getAmount(); //how much items were missing?
                                            assignee.getInventory().addItem(new ItemStack(itemID, amount-missingAmount, subID)); //give all already removed items back, because he does not have the required amount
                                            assignee.updateInventory(); //deprecated
                                            assignee.sendMessage(ChatColor.YELLOW + "Du hast nicht die benoetigten " + amount + " " + iStack.getType().getMaterial(itemID).toString() + ":" + String.valueOf(subID) + " um diesen Auftrag zu erfuellen.");
                                        }
                                        else //assignee has enough of the item to complete the assignment
                                        {
                                            if(plugin.getConfig().getBoolean("debug")){log.info(ChatColor.GREEN + "Du hast die noetigen Waren dabei. Sehr gut!");}                           
                                            EconomyResponse ecoRes = Assignment.econ.depositPlayer(assignee.getName(), reward); //give assignee his reward
                                            if(ecoRes.transactionSuccess())
                                            {
                                                String stillAvailable = "";
                                                int notFittingAmount = 0; //amount of Items that do not fit into the assigners inventory
                                                //assignee.getInventory().remove(iStack);                                                
                                                plugin.updateSignInDB(sign.getX(), sign.getY(), sign.getZ(), sign.getWorld().getName(), Assignment.assignmentStateCompleted); // change state so "completed"
                                                assignee.sendMessage(ChatColor.GREEN + "Danke! Dir wurden " + ChatColor.YELLOW + reward + " " + currency + ChatColor.GREEN + " ausgezahlt fuer deinen erledigten Job.");                                            
                                                //Assignee has got his reward and does no longer matter!

                                                //-------------------------------------------------------          

                                                // ---------- Handle Assigner payout -------
                                                if(null != (assigner = plugin.getAssignerFromDB((int)sign.getX(), (int)sign.getY(), (int)sign.getZ(), sign.getWorld().getName())))
                                                {// Assigner is currently online and now gets his items delivered                                                  
                                                    assigner.sendMessage(ChatColor.GREEN + assignee.getName() + " hat einen deiner Auftraege erledigt und dafuer ");
                                                    assigner.sendMessage(ChatColor.YELLOW + String.valueOf(reward) + " " + currency + ChatColor.GREEN +" von dir erhalten.");    
                                                    //try to add the items from the assignment to the assigners inventory                                                        
                                                    assignee.updateInventory(); //deprecated
                                                    HashMap<Integer, ItemStack> couldnotAdd = assigner.getInventory().addItem(iStack);
                                                    assigner.updateInventory(); //deprecated
                                                    if(false == couldnotAdd.isEmpty()) //not all items fitted in the assigners inventory
                                                    {                                                            
                                                        notFittingAmount = couldnotAdd.get(0).getAmount();  

                                                        if(subID > 0)
                                                        {
                                                            stillAvailable = String.valueOf(itemID) + ":" + String.valueOf(subID) + ":" + String.valueOf(notFittingAmount);
                                                            sign.setLine(1, stillAvailable);
                                                        }
                                                        else
                                                        {
                                                            stillAvailable = String.valueOf(itemID) + ":" + String.valueOf(notFittingAmount);
                                                            sign.setLine(1, stillAvailable);
                                                        }    
                                                        sign.setLine(0, "<" + Assignment.completedAssTag + ">");
                                                        sign.setLine(2, Assignment.rightClickText); //max. 15 Zeichen!     
                                                        sign.update();                                                        
                                                        // keep Name of Assigner in Line 4                                           

                                                        if(assigner.isOnline())
                                                        {
                                                            if(amount == notFittingAmount) //Players Inventory is full
                                                            {
                                                                assigner.sendMessage(ChatColor.YELLOW + "Dein Inventar ist voll! Lege etwas ab, um weitere Waren deines Auftrags einzusammeln.");
                                                            }
                                                            else if(subID > 0) //subID given
                                                            {                                  
                                                                assigner.sendMessage(ChatColor.GREEN + "Deinem Inventar wurden " + ChatColor.YELLOW + (amount-notFittingAmount) + " " + iStack.getType().getMaterial(itemID).toString() + ":" + String.valueOf(subID) + ChatColor.YELLOW + " hinzugefuegt.");
                                                                assigner.sendMessage(ChatColor.GREEN + "Du kannst weitere " + notFittingAmount + " " + iStack.getType().getMaterial(itemID).toString() + ":" + String.valueOf(subID) + " bei deinem Schild abholen.");
                                                                assigner.sendMessage(ChatColor.GREEN + " (Rechtsklick)");
                                                                assigner.sendMessage(ChatColor.GREEN + "Position des Schilds: X: " + String.valueOf((int)event.getClickedBlock().getX()) + "  Z: " + String.valueOf((int)event.getClickedBlock().getZ()) + " in " + event.getClickedBlock().getWorld().getName());
                                                            }
                                                            else
                                                            {                                  
                                                                assigner.sendMessage(ChatColor.GREEN + "Deinem Inventar wurden " + ChatColor.YELLOW + (amount-notFittingAmount) + " " + iStack.getType().getMaterial(itemID).toString() + ChatColor.GREEN + " hinzugefuegt.");
                                                                assigner.sendMessage(ChatColor.GREEN + "Du kannst weitere " + notFittingAmount + " " + iStack.getType().getMaterial(itemID).toString() + " bei deinem Schild abholen.");
                                                                assigner.sendMessage(ChatColor.GREEN + " (Rechtsklick)");
                                                                assigner.sendMessage(ChatColor.GREEN + "Position des Schilds: X: " + String.valueOf((int)event.getClickedBlock().getX()) + "  Z: " + String.valueOf((int)event.getClickedBlock().getZ()) + " in " + event.getClickedBlock().getWorld().getName());
                                                            }
                                                        }                                                       
                                                    }
                                                    else
                                                    {
                                                        assigner.sendMessage(ChatColor.GREEN + "Deinem Inventar wurden " + ChatColor.YELLOW + amount + " " + iStack.getType().getMaterial(itemID).toString() + ":" + String.valueOf(subID) + ChatColor.GREEN + " hinzugefuegt.");
                                                        event.getClickedBlock().setType(Material.AIR); //remove sign
                                                        plugin.deleteSignFromDB(event.getClickedBlock().getX(), event.getClickedBlock().getY(), event.getClickedBlock().getZ(), event.getClickedBlock().getWorld().getName()); //remove DB entry of sign
                                                        //Assignment is now fulfilled successfully    
                                                    }  
                                                }
                                                else // Assigner is currently not online
                                                {                                                    
                                                    // he will be informed about his fulfilled assignments upon login
                                                }

                                                // Set up sign for showing the completed assignment state
                                                //TODO könnte man auch zusätzlich per Kommando erlauben. /ass abholen / collect oder so....
                                                notFittingAmount = iStack.getAmount();
                                                if(subID > 0)
                                                {
                                                    stillAvailable = String.valueOf(itemID) + ":" + String.valueOf(subID) + ":" + String.valueOf(notFittingAmount);
                                                }
                                                else
                                                {
                                                    stillAvailable = String.valueOf(itemID) + ":" + String.valueOf(notFittingAmount);
                                                }                                                 
                                                sign.setLine(0, "<" + Assignment.completedAssTag + ">");
                                                sign.setLine(1, stillAvailable);
                                                sign.setLine(2, Assignment.rightClickText); //max. 15 Zeichen!     
                                                sign.update();
                                                //keep Name of Assigner in Line 4 
                                            }
                                            else
                                            {
                                                assignee.sendMessage(ChatColor.RED + "Leider gab es einen Fehler beim Ueberweisen des Geldes. Bitte melde das einem Admin!");
                                                log.severe(Assignment.logPrefix +"Error on transferring the reward to the assignee.");                                
                                            }
                                        }
                                    }
                                    else // player who clicked is an assignee, but assignment is already completed
                                    {
                                        event.getPlayer().sendMessage(ChatColor.YELLOW + "Dieser Auftrag wurde schon erfuellt.");
                                    }
                                }
                                else
                                {
                                    event.getPlayer().sendMessage(ChatColor.RED + "Du hast nicht die Berechtigung um Auftraege anzunehmen.");    
                                }                                    
                            }
                        }
                        catch (Exception e)
                        {
                            log.severe(Assignment.logPrefix + "An error occured while fetching sign position out of DB. ERROR: " + e.toString());                       
                        }  
                    }
                    else
                    {
                        event.getPlayer().sendMessage(ChatColor.YELLOW + "Dieses Schild ist ein Fake und gehoert nicht zu einem registrierten Auftrag!");
                    }
                }
            }            
        }
        else if(act == Action.LEFT_CLICK_BLOCK)
        {
            if(event.getClickedBlock().getTypeId() == 63 ||
                    event.getClickedBlock().getTypeId() == 68) // Left clicked a sign on a block (68) oder a signpost (63)?
            {
                sign = (Sign) event.getClickedBlock().getState();                

                if(sign.getLine(0).contains("<A>") || sign.getLine(0).contains("<a>")  || sign.getLine(0).contains("<" + Assignment.openAssignmentTitle + ">")) // is Assignment sign?
                {
                    String assignerName = "";
                    assignerName = plugin.getAssignerNameFromDB((int)sign.getX(), (int)sign.getY(), (int)sign.getZ(), sign.getWorld().getName());

                    if(assignerName != "") // sign is registered assignment sign
                    {
                        try
                        {
                            lineArray = sign.getLine(1).split(":"); //Format: amount:reward + $-Zeichen das noch weg muss!

                            if(lineArray.length == 2) //no subID given
                            {
                                itemID = Integer.parseInt(lineArray[0]);                                        
                                amount = Integer.parseInt(lineArray[1]);
                                String temp = sign.getLine(2).replace(currency, "").trim();
                                reward = Integer.parseInt(temp);  
                            }
                            else
                            {
                                itemID = Integer.parseInt(lineArray[0]);
                                subID = Short.parseShort(lineArray[1]);
                                amount = Integer.parseInt(lineArray[2]);
                                String temp = sign.getLine(2).replace(currency, "").trim();
                                reward = Integer.parseInt(temp);  
                            }        
                            event.getPlayer().sendMessage(ChatColor.GREEN + "Der Auftraggeber " + ChatColor.YELLOW + sign.getLine(3) + ChatColor.GREEN + " bezahlt " + String.valueOf(reward) + " " + currency + " fuer " + String.valueOf(amount) + " " + Material.getMaterial(itemID).toString() + ":" + String.valueOf(subID) + ".");
                        }
                        catch(Exception e)
                        {
                            if(plugin.getConfig().getBoolean("debug")){log.info("Fehler beim parsen des Schilds. " + e.toString());}
                            event.getPlayer().sendMessage(ChatColor.RED + "Dieses Schild ist nicht vollstaendig oder nicht korrekt.");
                        }                    
                    }
                    else //sign is a fake
                    {
                        event.getPlayer().sendMessage(ChatColor.YELLOW + "Dieses Schild ist ein Fake und gehoert nicht zu einem registrierten Auftrag!");
                    }
                }

            }
        }        
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false) // -> priority MUST BE HIGHER to protect the sign
    public void onBlockBreak(BlockBreakEvent event) //For the Assignee
    {        
        if(plugin.getConfig().getBoolean("debug")){log.info("im onBlockBreakEvent");}
        if(event.getBlock().getTypeId() == 63 ||
                event.getBlock().getTypeId() == 68) // a sign on a sign on block (68) oder a signpost (63)?
        {
            if(plugin.getConfig().getBoolean("debug")){log.info("Sign erkannt");}
            Sign bSign = (Sign)event.getBlock().getState();
            if(plugin.getConfig().getBoolean("debug")){log.info("Sign Line 0: " + bSign.getLine(0));}

            if(bSign.getLine(0).contains("<A>") || bSign.getLine(0).contains("<a>") || bSign.getLine(0).contains("<" + Assignment.completedAssTag + ">") || bSign.getLine(0).contains("<" + Assignment.openAssignmentTitle + ">")) // is Assignment sign?
            {
                String assignerName = plugin.getAssignerNameFromDB((int)event.getBlock().getX(), (int)event.getBlock().getY(), (int)event.getBlock().getZ(), event.getBlock().getWorld().getName());
                assigner = plugin.getAssignerFromDB((int)event.getBlock().getX(), (int)event.getBlock().getY(), (int)event.getBlock().getZ(), event.getBlock().getWorld().getName());

                if("" != assignerName) // sign is registered assignment sign
                {            
                    if(plugin.getConfig().getBoolean("debug")){log.info("Assignment Sign erkannt");}

                    try
                    {   
                        if(event.getPlayer().getName().equalsIgnoreCase(assignerName) ||
                                event.getPlayer().hasPermission("assignment.break") ||
                                event.getPlayer().hasPermission("assignment.*")) //player is the assigner or a player with permission to break the sign
                        {     
                            if(bSign.getLine(0).contains("<" + Assignment.completedAssTag + ">")) //he cancels an allready fullfilled asignment and loses his not yet picked up items and gains no money back
                            { 
                                //delete sign from DB
                                String dquery = "DELETE FROM signs WHERE x=" + (int)bSign.getX() + " AND y=" + (int)event.getBlock().getY() + " AND z=" + (int)bSign.getZ() + " AND world='" + event.getPlayer().getWorld().getName() + "'";
                                plugin.manageSQLite.deleteQuery(dquery);
                                event.getPlayer().sendMessage(ChatColor.YELLOW + "Die restlichen Items sind hiermit verfallen.");
                                event.getPlayer().sendMessage(ChatColor.YELLOW + "Dieser Auftrag wurde geloescht.");
                                if(plugin.getConfig().getBoolean("debug")){log.info("player has permission to break it");}
                            }
                            else // he cancels the unfullfilled assignment, so pay his money back
                            {
                                try
                                {
                                    String query = "SELECT reward FROM signs WHERE x=" + (int)bSign.getX() + " AND y=" + (int)event.getBlock().getY() + " AND z=" + (int)bSign.getZ() + " AND world='" + event.getPlayer().getWorld().getName() + "'"; 
                                    ResultSet resSet = plugin.manageSQLite.sqlQuery(query);
                                    resSet.next(); //set pointer to first row
                                    int payback = resSet.getInt(resSet.getRow());
                                    EconomyResponse ecoRes = Assignment.econ.depositPlayer(assigner.getName(), payback); //payback reward
                                    if(ecoRes.transactionSuccess()) 
                                    {
                                        if((null != assigner) && (assigner.isOnline()))
                                        {
                                            assigner.sendMessage(ChatColor.YELLOW + "Du hast " + String.valueOf(payback) + " " + currency + " zurueckerstattet bekommen.");    
                                        }                                        
                                        //delete sign from DB                                        
                                        plugin.deleteSignFromDB((int)bSign.getX(), (int)bSign.getY(), (int)bSign.getZ(), bSign.getWorld().getName());
                                        event.getPlayer().sendMessage(ChatColor.YELLOW + "Dieser Auftrag wurde hiermit geloescht.");
                                        if(plugin.getConfig().getBoolean("debug")){log.info("player has permission to break it");}
                                    }
                                }
                                catch (Exception e)
                                {
                                    log.severe(Assignment.logPrefix + "Error on payback of the assigner. Could not find sign in DB." + e);
                                }
                            }
                        }
                        else
                        {
                            event.setCancelled(true); //cancel BlockBreak if its a registered Assignment sign and it's not the assigner or an OP who tries to break it
                            event.getPlayer().sendMessage(ChatColor.RED + "Du hast keine Berechtigung um dieses Auftragsschild abzureissen!");    
                        }  
                    }
                    catch (Exception e)
                    {
                        log.severe(Assignment.logPrefix + "An error occured while fetching sign out of DB at BlockBreakEvent. ERROR: " + e.toString());                       
                    }
                }
                else
                {
                    event.getPlayer().sendMessage(ChatColor.YELLOW + "Dieses Schild ist ein Fake und gehoert nicht zu einem registrierten Auftrag!");
                }
            }
        }
    }

    // Needed to prevent dupe bug when assignee clicks Assignment sign with the Block that is requested by the assignment in hand.
    // The block would be placed without beeing removed from his inventory properly, causing a dupe. So placing must be blocked in this case.
    // also blocks accidnental placing of blocks when using right click with block in hand
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false) 
    public void onBlockPlace(BlockPlaceEvent event) //For the Assignee
    {
        if(plugin.blockNextBlockPlacing)
        {
            event.setCancelled(true); //prevent placing as long as Assignee-Handling is running to prevent duping blocks.
            plugin.blockNextBlockPlacing = false; //release blockPlacing lock
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) // event has MONITOR priority and will be skipped if it has been cancelled before
    public void onPlayerJoin(PlayerJoinEvent event)
    {      
        final PlayerJoinEvent pjEvent = event;        

        // Check if player has unfulfilled assignments to pick up
        final int assRdyForPickup = plugin.getCompletedAssignmentsFromDB(event.getPlayer().getName());

        if(assRdyForPickup > 0)
        {
            if(assRdyForPickup == 1) // there are completed assignments for this player to pick up
            { 
                // DELAYED SYNCH TASK (only called once)
                plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable()
                {
                    public void run()
                    {
                        pjEvent.getPlayer().sendMessage(ChatColor.GREEN + "Du hast noch einen erfuellten Auftrag. Du kannst am Schild deine Waren abholen.");
                        pjEvent.getPlayer().sendMessage(ChatColor.GREEN + "Mit " + ChatColor.WHITE + "/ass list" + ChatColor.GREEN + " kannst du deine Auftraege ansehen.");
                    }
                }, 200L); 

            }
            else
            {
                // DELAYED SYNCH TASK (only called once)
                plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable()
                {
                    public void run()
                    {
                        pjEvent.getPlayer().sendMessage(ChatColor.GREEN + "Du hast noch " + Integer.toString(assRdyForPickup) + " erfuellte Auftraege. Bitte hole deine Waren ab.");
                        pjEvent.getPlayer().sendMessage(ChatColor.GREEN + "Mit " + ChatColor.WHITE + "/ass list" + ChatColor.GREEN + " kannst du deine Auftraege ansehen.");
                    }
                }, 200L);                
            } 
        }
        // --------------------------------------------             
    }
}
