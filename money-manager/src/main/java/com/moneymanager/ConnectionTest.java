package com.moneymanager;

import com.moneymanager.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class ConnectionTest {
    public static void main(String[] args) {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 AS result")) {

            if (rs.next()) {
                System.out.println("Connection successful! SELECT 1 returned: " + rs.getInt("result"));
            }
        } catch (Exception e) {
            System.err.println("Connection FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
