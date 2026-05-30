package com.moneymanager;

import com.moneymanager.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectionTest {
    private static final Logger LOGGER = Logger.getLogger(ConnectionTest.class.getName());

    public static void main(String[] args) {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 AS result")) {

            if (rs.next()) {
                System.out.println("Connection successful! SELECT 1 returned: " + rs.getInt("result"));
            }
        } catch (Exception e) {
            System.err.println("Connection FAILED: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Connection failed", e);
            System.exit(1);
        }
    }
}
