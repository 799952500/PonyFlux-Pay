package com.payflow.admin.tools;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 本机数据库迁移 + 冒烟辅助（仅开发环境使用）。
 *
 * <p>用途：</p>
 * <ul>
 *     <li>在未安装 mysql 客户端时，使用 JDBC 执行 SQL 文件</li>
 *     <li>按表名前缀把 SQL 分发到 payflow_admin / payflow_cashier</li>
 * </ul>
 */
public final class LocalDbMigrateAndSmoke {

    private static final String ADMIN_URL =
            "jdbc:mysql://127.0.0.1:3306/payflow_admin?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
    private static final String CASHIER_URL =
            "jdbc:mysql://127.0.0.1:3306/payflow_cashier?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    public static void main(String[] args) throws Exception {
        System.out.println("[LocalDbMigrateAndSmoke] start at " + LocalDateTime.now());

        try (Connection admin = DriverManager.getConnection(ADMIN_URL, USER, PASSWORD);
             Connection cashier = DriverManager.getConnection(CASHIER_URL, USER, PASSWORD)) {

            execFileRouted(admin, cashier, "sql/migrations/20260507-plan-extensions.sql");
            execFileOn(cashier, "sql/migrations/20260507-union-channel-seed.sql");
            execFileOn(admin, "sql/migrations/20260507-admin-feature-menus.sql");

            ensureAdminLoginUser(admin);
            seedPaymentLinkForPublicSmoke(cashier);
        }

        System.out.println("[LocalDbMigrateAndSmoke] done at " + LocalDateTime.now());
    }

    private static void execFileOn(Connection conn, String relPath) throws Exception {
        System.out.println("[SQL] execute on single db: " + relPath);
        for (String sql : splitSqlFile(relPath)) {
            execOne(conn, sql);
        }
    }

    private static void execFileRouted(Connection admin, Connection cashier, String relPath) throws Exception {
        System.out.println("[SQL] execute routed: " + relPath);
        for (String sql : splitSqlFile(relPath)) {
            Connection target = route(sql) ? cashier : admin;
            execOne(target, sql);
        }
    }

    /**
     * @return true 表示走 cashier；false 表示走 admin
     */
    private static boolean route(String sql) {
        String s = sql.toLowerCase();
        if (s.contains("cashier_")) {
            return true;
        }
        if (s.contains("payment_link")) {
            return true;
        }
        return false;
    }

    private static void execOne(Connection conn, String sql) throws SQLException {
        if (sql == null || sql.isBlank()) {
            return;
        }
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            // 幂等：重复列/表等忽略（本机反复执行常见）
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.contains("Duplicate column name")
                    || msg.contains("already exists")
                    || msg.contains("Duplicate entry")
                    || msg.contains("Table") && msg.contains("already exists")) {
                System.out.println("[SQL][SKIP] " + msg);
                return;
            }
            System.out.println("[SQL][FAIL] " + msg);
            throw e;
        }
    }

    private static List<String> splitSqlFile(String relPath) throws Exception {
        String abs = "D:/个人/pay/PonyFlux-Pay/" + relPath;
        List<String> out = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(abs), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.startsWith("--")) {
                    continue;
                }
                buf.append(line).append('\n');
                if (trimmed.endsWith(";")) {
                    String sql = buf.toString().trim();
                    buf.setLength(0);
                    sql = sql.substring(0, sql.length() - 1).trim();
                    if (!sql.isBlank()) {
                        out.add(sql);
                    }
                }
            }
        }
        if (!buf.toString().trim().isEmpty()) {
            out.add(buf.toString().trim());
        }
        return out;
    }

    private static void ensureAdminLoginUser(Connection admin) throws SQLException {
        // admin/admin123（BCrypt）
        String sql = """
                INSERT IGNORE INTO admin_users (username, password, role, nickname, status, created_at, updated_at)
                VALUES ('admin', '$2a$10$Yim93NmIRVYlKFwWYsbWqOpL36En0hfvui.KmREpkF51GiEHvL2im', 'SUPER_ADMIN', '超级管理员', 'ACTIVE', NOW(), NOW())
                """;
        execOne(admin, sql);
    }

    private static void seedPaymentLinkForPublicSmoke(Connection cashier) throws SQLException {
        String sql = """
                INSERT IGNORE INTO payment_link (link_id, merchant_id, title, amount, currency, max_use, used_count, expire_at, status, created_at)
                VALUES ('LNKSMOKE01', 'M2024040001', '冒烟测试链接', 100, 'CNY', 10, 0, NULL, 'ACTIVE', NOW())
                """;
        execOne(cashier, sql);
    }
}

