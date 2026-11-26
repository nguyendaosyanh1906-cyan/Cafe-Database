package com.cafe.cafeshopmanagement;

import com.cafe.cafeshopmanagement.dao.DataAccessException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** External configuration only; credentials are never logged. */
public final class DBConnection {
    private DBConnection() {}
    private static String setting(String property, String environment, String fallback) {
        String value = System.getProperty(property);
        if (value == null) value = System.getenv(environment);
        return value == null ? fallback : value;
    }
    public static Connection getConnection() {
        String url = setting("cafe.db.url", "CAFE_DB_URL", "jdbc:mysql://localhost:3306/cafe_management_db");
        String user = setting("cafe.db.user", "CAFE_DB_USER", null);
        String password = setting("cafe.db.password", "CAFE_DB_PASSWORD", null);
        if (user == null || user.isBlank() || password == null) {
            throw new DataAccessException("Set CAFE_DB_USER and CAFE_DB_PASSWORD (or cafe.db.user/password)");
        }
        try { return DriverManager.getConnection(url, user, password); }
        catch (SQLException e) { throw new DataAccessException("Database connection failed", e); }
    }
}
