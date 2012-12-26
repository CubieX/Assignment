package com.alta189.sqlLibrary.SQLite;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.libs.jline.internal.Log;

import com.github.CubieX.Assignment.Assignment;

public class DatabaseHandler {
	/*
	 * @author: alta189
	 * 
	 */
	
	private sqlCore core;
	private Connection connection;
	private File SQLFile;
	private Assignment plugin;
	private Logger log;
	
	public DatabaseHandler(sqlCore core, File SQLFile, Assignment plugin, Logger log)
	{
		this.core = core;
		this.SQLFile = SQLFile;
		this.plugin = plugin;
		this.log = log;
	}

	public Connection getConnection() {
		if (connection == null) {
		      initialize();
		}
		return connection;
	}
	
	public void closeConnection() {
	    if (this.connection != null)
	      try {
	        this.connection.close();
	      } catch (SQLException ex) {
	        this.core.writeError("Error on Connection close: " + ex, true);
	      }
	  }
	
	public Boolean initialize() {
		try {
	      Class.forName("org.sqlite.JDBC");
	      connection = DriverManager.getConnection("jdbc:sqlite:" + SQLFile.getAbsolutePath());
	      return true;
	    } catch (SQLException ex) {
	      core.writeError("SQLite exception on initialize " + ex, true);
	    } catch (ClassNotFoundException ex) {
	      core.writeError("You need the SQLite library " + ex, true);
	    }
	    return false;
	}
	
	public Boolean createTable(String query) {
		try {
			if (query == null) { core.writeError("SQL Create Table query empty.", true); return false; }
		    
			Statement statement = connection.createStatement();
		    statement.execute(query);
		    return true;
		} catch (SQLException ex){
			core.writeError(ex.getMessage(), true);
			return false;
		}
	}
	
	public ResultSet sqlQuery(String query)
	{
		try
		{
			Connection connection = getConnection();
		    Statement statement = connection.createStatement();
		    
		    ResultSet result = statement.executeQuery(query);
		    
		    return result;
		}
		catch (SQLException ex)
		{
			if (ex.getMessage().toLowerCase().contains("locking") || ex.getMessage().toLowerCase().contains("locked")) {
				return retryResult(query);
			}else{
				core.writeError("Error at SQL Query: " + ex.getMessage(), false);
			}			
		}
		return null;
	}
	
	public void insertQuery(final String query)
	{ // execute query asynchronous, so main thread is not blocked by SQL database operations. (Insert, Update, Delete)
	    Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable()
	    {
	        @Override
	        public void run()
	        {
	            long startTime = ((Calendar)Calendar.getInstance()).getTimeInMillis();
	            
	            try
	            {
	                Connection connection = getConnection();
	                Statement statement = connection.createStatement();

	                statement.executeQuery(query);
	            }
	            catch (SQLException ex)
	            {

	                if (ex.getMessage().toLowerCase().contains("locking") || ex.getMessage().toLowerCase().contains("locked")) {
	                    retry(query);
	                }else{
	                    if (!ex.toString().contains("not return ResultSet")) core.writeError("Error at SQL INSERT Query: " + ex, false);
	                }
	            }
	            
	            if(Assignment.debug)
                {
                    log.info(Assignment.logPrefix + "INSERT-Query executed in: " + (((Calendar)Calendar.getInstance()).getTimeInMillis() - startTime) + " milliseconds.");
                }
	        }
	    });
	}
	
	public void updateQuery(final String query)
	{
	 // execute query asynchronous, so main thread is not blocked by SQL database operations. (Insert, Update, Delete)
        Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable()
        {
            @Override
            public void run()
            {
                long startTime = ((Calendar)Calendar.getInstance()).getTimeInMillis();
                
                try {
                    Connection connection = getConnection();
                    Statement statement = connection.createStatement();
                    
                    statement.executeQuery(query);                    
                    
                } catch (SQLException ex) {
                    if (ex.getMessage().toLowerCase().contains("locking") || ex.getMessage().toLowerCase().contains("locked")) {
                        retry(query);
                    }else{
                        if (!ex.toString().contains("not return ResultSet")) core.writeError("Error at SQL UPDATE Query: " + ex, false);
                    }
                }
                
                if(Assignment.debug)
                {
                    log.info(Assignment.logPrefix + "UPDATE-Query executed in: " + (((Calendar)Calendar.getInstance()).getTimeInMillis() - startTime) + " milliseconds.");
                }
            }
        });		
	}
	
	public void deleteQuery(final String query)
	{   // execute query asynchronous, so main thread is not blocked by SQL database operations. (Insert, Update, Delete)
	    Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable()
	    {
	        @Override
	        public void run()
	        {
	            long startTime = ((Calendar)Calendar.getInstance()).getTimeInMillis();

	            try
	            {
	                Connection connection = getConnection();
	                Statement statement = connection.createStatement();

	                statement.executeQuery(query);	                    
	            }
	            catch (SQLException ex) {
	                if (ex.getMessage().toLowerCase().contains("locking") || ex.getMessage().toLowerCase().contains("locked")) {
	                    retry(query);
	                }else{
	                    if (!ex.toString().contains("not return ResultSet")) core.writeError("Error at SQL DELETE Query: " + ex, false);
	                }
	            }
	            
	            if(Assignment.debug)
	            {
	                log.info(Assignment.logPrefix + "DELETE-Query executed in: " + (((Calendar)Calendar.getInstance()).getTimeInMillis() - startTime) + " milliseconds.");
	            }
	        }
	    });		
	}
	
	public Boolean wipeTable(String table) {
		try {
			if (!core.checkTable(table)) {
				core.writeError("Error at Wipe Table: table, " + table + ", does not exist", true);
				return false;
			}
			Connection connection = getConnection();
		    Statement statement = connection.createStatement();
		    String query = "DELETE FROM " + table + ";";
		    statement.executeQuery(query);
		    
		    return true;
		} catch (SQLException ex) {
			if (ex.getMessage().toLowerCase().contains("locking") || ex.getMessage().toLowerCase().contains("locked")) {
				//retryWipe(query);
			}else{
				if (!ex.toString().contains("not return ResultSet")) core.writeError("Error at SQL WIPE TABLE Query: " + ex, false);
			}
			return false;
		}
	}
	
	
	public Boolean checkTable(String table) {
		DatabaseMetaData dbm;
		try {
			dbm = this.getConnection().getMetaData();
			ResultSet tables = dbm.getTables(null, null, table, null);
			if (tables.next()) {
			  return true;
			}
			else {
			  return false;
			}
		} catch (SQLException e) {			
			core.writeError("Failed to check if table \"" + table + "\" exists: " + e.getMessage(), true);
			return false;
		}
		
	}
	
	private ResultSet retryResult(String query) {
		Boolean passed = false;
		
		while (!passed) {
			try {
				Connection connection = getConnection();
			    Statement statement = connection.createStatement();
			    
			    ResultSet result = statement.executeQuery(query);
			    
			    passed = true;
			    
			    return result;
			} catch (SQLException ex) {
				
				if (ex.getMessage().toLowerCase().contains("locking") || ex.getMessage().toLowerCase().contains("locked")) {
					passed = false;
				}else{
					core.writeError("Error at SQL Query: " + ex.getMessage(), false);
				}
			}
		}
		
		return null;
	}
	
	private void retry(String query) {
		Boolean passed = false;
		
		while (!passed) {
			try {
				Connection connection = getConnection();
			    Statement statement = connection.createStatement();
			    
			    statement.executeQuery(query);
			    
			    passed = true;
			    
			    return;
			} catch (SQLException ex) {
				
				if (ex.getMessage().toLowerCase().contains("locking") || ex.getMessage().toLowerCase().contains("locked") ) {
					passed = false;
				}else{
					core.writeError("Error at SQL Query: " + ex.getMessage(), false);
				}
			}
		}
		
		return;
	}
}
