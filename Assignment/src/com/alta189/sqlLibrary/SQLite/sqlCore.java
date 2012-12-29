package com.alta189.sqlLibrary.SQLite;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.logging.Logger;

import com.github.CubieX.Assignment.Assignment;

public class sqlCore {

   /*
    *  @author: alta189
    * 
    */

   private Logger log;
   private Assignment plugin;
   public String dbLocation;
   public String dbName;
   private DatabaseHandler manageDB;

   public sqlCore(Logger log, String dbName, String dbLocation, Assignment plugin) {
      this.log = log;
      this.dbName = dbName;
      this.dbLocation = dbLocation;
      this.plugin = plugin;
   }

   public void writeInfo(String toWrite) {
      if (toWrite != null) {
         this.log.info(Assignment.logPrefix + toWrite);
      }
   }

   public void writeError(String toWrite, Boolean severe) {
      if (severe) {
         if (toWrite != null) {
            this.log.severe(Assignment.logPrefix + toWrite);
         }
      } else {
         if (toWrite != null) {
            this.log.warning(Assignment.logPrefix + toWrite);
         }
      }
   }

   public Boolean initialize() {
      File dbFolder = new File(dbLocation);
      if (dbName.contains("/") || dbName.contains("\\") || dbName.endsWith(".db")) { this.writeError("The database name can not contain: /, \\, or .db", true); return false; }
      if (!dbFolder.exists()) {
         dbFolder.mkdir();
      }

      File SQLFile = new File(dbFolder.getAbsolutePath() + "/" + dbName);

      this.manageDB = new DatabaseHandler(this, SQLFile, plugin, log);

      return this.manageDB.initialize();
   }

   public ResultSet sqlQuery(String query) {
      return this.manageDB.sqlQuery(query);
   }

   public Boolean createTable(String query) {
      return this.manageDB.createTable(query);
   }

   public void insertQuery(String query) {
      this.manageDB.insertQuery(query);
   }

   public void updateQuery(String query) {
      this.manageDB.updateQuery(query);
   }

   public void deleteQuery(String query) {
      this.manageDB.deleteQuery(query);
   }

   public Boolean checkTable(String table) {
      return this.manageDB.checkTable(table);
   }

   public Boolean wipeTable(String table) {
      return this.manageDB.wipeTable(table);
   }

   public Connection getConnection() {
      return this.manageDB.getConnection();
   }

   public void close() {
      this.manageDB.closeConnection();
   }

   public Boolean checkConnection() {
      Connection con = this.manageDB.getConnection();

      if (con != null) {
         return true;
      } 
      return false;
   }
}
