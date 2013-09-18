package me.stutiguias.webportal.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import me.stutiguias.webportal.init.WebPortal;
import me.stutiguias.webportal.plugins.ProfileMcMMO;
import me.stutiguias.webportal.settings.*;
import org.bukkit.inventory.ItemStack;

public class MySQLDataQueries extends Queries {

        private WALConnectionPool pool;
        
	public MySQLDataQueries(WebPortal plugin, String dbHost, String dbPort, String dbUser, String dbPass, String dbName) {
		super(plugin);
                try {
                        WebPortal.logger.log(Level.INFO, "{0} Starting pool....", plugin.logPrefix);
                        pool = new WALConnectionPool("com.mysql.jdbc.Driver", "jdbc:mysql://"+ dbHost +":"+ dbPort +"/"+ dbName, dbUser, dbPass);
                }catch(InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException e) {
                        WebPortal.logger.log(Level.WARNING, "{0} Exception getting mySQL WALConnection", plugin.logPrefix);
			WebPortal.logger.warning(e.getMessage());
                }
	}

        @Override
	public WALConnection getConnection() {
		try {
			return pool.getConnection();
		} catch (Exception e) {
			WebPortal.logger.log(Level.WARNING, "{0} Exception getting mySQL WALConnection", plugin.logPrefix);
			WebPortal.logger.warning(e.getMessage());
		}
		return null;
	}
        
	public boolean tableExists(String tableName) {
		boolean exists = false;
		WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;
		try {
			st = conn.prepareStatement("SHOW TABLES LIKE ?");
			st.setString(1, tableName);
			rs = st.executeQuery();
			while (rs.next()) {
				exists = true;
			}
		} catch (SQLException e) {
			WebPortal.logger.log(Level.WARNING, "{0} Unable to check if table exists: {1}", new Object[]{plugin.logPrefix, tableName});
			WebPortal.logger.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
		return exists;
	}

        @Override
	public void initTables() {
		if (!tableExists("WA_Players")) {
			WebPortal.logger.log(Level.INFO, "{0} Creating table WA_Players", plugin.logPrefix);
			executeRawSQL("CREATE TABLE WA_Players (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), name VARCHAR(255), pass VARCHAR(255), money DOUBLE, itemsSold INT, itemsBought INT, earnt DOUBLE, spent DOUBLE, canBuy INT, canSell INT, isAdmin INT);");
		}
		if (!tableExists("WA_StorageCheck")) {
			WebPortal.logger.log(Level.INFO, "{0} Creating table WA_StorageCheck", plugin.logPrefix);
			executeRawSQL("CREATE TABLE WA_StorageCheck (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), time INT);");
		}
		if (!tableExists("WA_Auctions")) {
			WebPortal.logger.log(Level.INFO, "{0} Creating table WA_Auctions", plugin.logPrefix);
			executeRawSQL("CREATE TABLE WA_Auctions (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), name INT, damage INT, player VARCHAR(255), quantity INT, price DOUBLE, created INT, allowBids BOOLEAN Default '0', currentBid DOUBLE, currentWinner VARCHAR(255), ench VARCHAR(45), tableid INT(1));");
		}
		if (!tableExists("WA_SellPrice")) {
			WebPortal.logger.log(Level.INFO, "{0} Creating table WA_SellPrice", plugin.logPrefix);
			executeRawSQL("CREATE TABLE WA_SellPrice (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), name INT, damage INT, time INT, quantity INT, price DOUBLE, seller VARCHAR(255), buyer VARCHAR(255), ench VARCHAR(45));");
		}
		if (!tableExists("WA_MarketPrices")) {
			WebPortal.logger.log(Level.INFO, "{0} Creating table WA_MarketPrices", plugin.logPrefix);
			executeRawSQL("CREATE TABLE WA_MarketPrices (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), name INT, damage INT, time INT, marketprice DOUBLE, ref INT);");
		}
		if (!tableExists("WA_SaleAlerts")) {
			WebPortal.logger.log(Level.INFO, "{0} Creating table WA_SaleAlerts", plugin.logPrefix);
			executeRawSQL("CREATE TABLE WA_SaleAlerts (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), seller VARCHAR(255), quantity INT, price DOUBLE, buyer VARCHAR(255), item VARCHAR(255), alerted BOOLEAN Default '0');");
		}
                if (!tableExists("WA_DbVersion")) {
                        WebPortal.logger.log(Level.INFO, "{0} Creating table WA_DbVersion", plugin.logPrefix);
                        executeRawSQL("CREATE TABLE WA_DbVersion (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), dbversion INT);");
                        executeRawSQL("INSERT INTO WA_DbVersion (dbversion) VALUES (1)");
                        executeRawSQL("ALTER TABLE WA_Auctions ADD COLUMN `type` VARCHAR(45) NULL AFTER `tableid` , ADD COLUMN `itemname` VARCHAR(45) NULL  AFTER `type`, ADD COLUMN `searchtype` VARCHAR(45) NULL  AFTER `itemname` ;");
                }
                if (tableVersion() == 1) {
                        WebPortal.logger.log(Level.INFO, "{0} Update DB version to 2", plugin.logPrefix);
                        executeRawSQL("ALTER TABLE WA_Players ADD COLUMN `lock` VARCHAR(1) Default 'N' AFTER `isAdmin` ");
                        executeRawSQL("UPDATE WA_DbVersion SET dbversion = 2 where id = 1");
                }
                if (tableVersion() == 2) {
                        WebPortal.logger.log(Level.INFO, "{0} Update DB version to 3", plugin.logPrefix);
                        executeRawSQL("ALTER TABLE WA_Auctions DROP COLUMN `Itemname`, DROP COLUMN `allowBids`, DROP COLUMN `currentBid`, DROP COLUMN `currentWinner`;");
                        executeRawSQL("CREATE TABLE WA_ItemExtraInfo (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), auctionId INT, type VARCHAR(45), value TEXT );");
                        executeRawSQL("UPDATE WA_DbVersion SET dbversion = 3 where id = 1");
                }
                if (tableVersion() == 3) {
                        WebPortal.logger.log(Level.INFO, "{0} Update DB version to 4", plugin.logPrefix);
                        executeRawSQL("ALTER TABLE WA_Players DROP COLUMN `money`;");
                        executeRawSQL("ALTER TABLE WA_Players ADD COLUMN `webban` VARCHAR(1) Default 'N' AFTER `lock` ;");
                        executeRawSQL("UPDATE WA_DbVersion SET dbversion = 4 where id = 1");
                }
	}


        

        
        @Override
        public String getPassword(String player) {
                WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;
                String pass = null;

		try {
                        if(plugin.authplugin.equalsIgnoreCase("WebPortal")) {
                            st = conn.prepareStatement("SELECT pass FROM WA_Players WHERE name = ?");
                        }else if(plugin.authplugin.equalsIgnoreCase("xAuth")){
                            st = conn.prepareStatement("SELECT id FROM " + plugin.Table + " WHERE " + plugin.Username + " = ?");
                        } else {
                            st = conn.prepareStatement("SELECT " + plugin.ColumnPassword + " FROM " + plugin.Table + " WHERE " + plugin.Username + " = ?");
                        }
			st.setString(1, player);
			rs = st.executeQuery();
                        while (rs.next()) {
                            if(plugin.authplugin.equalsIgnoreCase("WebPortal")) {
                                pass = rs.getString("pass");
                            }else if(plugin.authplugin.equalsIgnoreCase("xAuth")) {
                                pass = rs.getString("id");
                            }else{
                                pass = rs.getString(plugin.ColumnPassword);   
                            }         
                        }
		} catch (SQLException e) {
			WebPortal.logger.log(Level.WARNING, "{0} Unable to update player permissions in DB", plugin.logPrefix);
			WebPortal.logger.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
                return pass;
        }

        @Override
        public ProfileMcMMO getMcMMOProfileMySql(String tableprefix,String player){ 
                ProfileMcMMO pf = null;
            
            	WALConnection conn = getConnection();
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			st = conn.prepareStatement("SELECT taming,mining,repair,unarmed,herbalism,excavation,archery,swords,axes,acrobatics,fishing FROM "+ tableprefix +"skills INNER JOIN "+ tableprefix +"users ON "+ tableprefix +"skills.user_id = "+ tableprefix +"users.id WHERE user = ?");
			st.setString(1, player);
			rs = st.executeQuery();
			while (rs.next()) {
				pf = new ProfileMcMMO();
				pf.setEXCAVATION(rs.getInt("excavation"));
                                pf.setTAMING(rs.getInt("taming"));
                                pf.setMINING(rs.getInt("mining"));
                                pf.setREPAIR(rs.getInt("repair"));
                                pf.setUNARMED(rs.getInt("unarmed"));
                                pf.setHERBALISM(rs.getInt("herbalism"));
                                pf.setARCHERY(rs.getInt("archery"));
                                pf.setSWORDS(rs.getInt("swords"));
                                pf.setAXES(rs.getInt("axes"));
                                pf.setACROBATICS(rs.getInt("acrobatics"));
                                pf.setFISHING(rs.getInt("fishing"));
			}
		} catch (SQLException e) {
			WebPortal.logger.log(Level.WARNING, "{0} Unable to get profile {1}", new Object[]{plugin.logPrefix, player});
			WebPortal.logger.warning(e.getMessage());
		} finally {
			closeResources(conn, st, rs);
		}
		return pf;
        }
    
}
