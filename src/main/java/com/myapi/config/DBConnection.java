package com.myapi.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {
    
    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("org.postgresql.Driver");
        
        // Get DATABASE_URL from Render environment
        String dbUrl = System.getenv("DATABASE_URL");
        
        if (dbUrl == null || dbUrl.isEmpty()) {
            throw new SQLException("DATABASE_URL environment variable is not set");
        }
        
        // Ensure proper JDBC format
        if (!dbUrl.startsWith("jdbc:")) {
            if (dbUrl.startsWith("postgresql://")) {
                dbUrl = "jdbc:" + dbUrl;
            } else if (dbUrl.startsWith("postgres://")) {
                dbUrl = "jdbc:" + dbUrl.replace("postgres://", "postgresql://");
            }
        }
        
        // Ensure port is specified
        if (!dbUrl.matches(".*:\\d+.*")) {
            dbUrl = dbUrl.replaceFirst("(@[^:]+)(/|$)", "$1:5432$2");
        }
        
        Connection conn = DriverManager.getConnection(dbUrl);
        
        // Auto-create tables if they don't exist
        createTablesIfNotExist(conn);
        
        return conn;
    }
    
    private static void createTablesIfNotExist(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // Users table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                "id SERIAL PRIMARY KEY," +
                "name VARCHAR(100) NOT NULL," +
                "email VARCHAR(100) NOT NULL UNIQUE," +
                "password VARCHAR(255) NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
            
            // Events table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS events (" +
                "id SERIAL PRIMARY KEY," +
                "event_name VARCHAR(200) NOT NULL," +
                "event_description TEXT," +
                "event_venue VARCHAR(200) NOT NULL," +
                "date DATE NOT NULL," +
                "user_id INTEGER NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")"
            );
            
            System.out.println("Tables verified/created successfully");
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
        }
    }
}