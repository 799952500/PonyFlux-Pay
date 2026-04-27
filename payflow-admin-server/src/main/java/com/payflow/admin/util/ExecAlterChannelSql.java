package com.payflow.admin.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * One-off utility to ALTER TABLE admin_channels ADD COLUMN icon.
 * Run via: mvn exec:java -Dexec.mainClass="com.payflow.admin.util.ExecAlterChannelSql"
 */
public class ExecAlterChannelSql {

    private static final String URL = "jdbc:mysql://127.0.0.1:3306/payflow_admin?characterEncoding=utf-8&useSSL=false";
    private static final String USER = "root";
    private static final String PASS = "root";

    public static void main(String[] args) {
        String sql = "ALTER TABLE admin_channels ADD COLUMN icon VARCHAR(512) DEFAULT NULL";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("✅ ALTER TABLE executed successfully: added column `icon` to `admin_channels`");
        } catch (Exception e) {
            // Column might already exist — treat as success
            if (e.getMessage() != null && e.getMessage().contains("Duplicate column name")) {
                System.out.println("ℹ️ Column `icon` already exists in `admin_channels`, skipping.");
            } else {
                System.err.println("❌ Failed to execute ALTER TABLE: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}
