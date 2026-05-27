package com.myapi.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    
    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("org.postgresql.Driver");
        
        // Get DATABASE_URL from Render environment
        String dbUrl = System.getenv("DATABASE_URL");
        
        // If DATABASE_URL is not found, try the public URL
        if (dbUrl == null || dbUrl.isEmpty()) {
            dbUrl = System.getenv("DATABASE_PUBLIC_URL");
        }
        
        // If still not found, throw an error
        if (dbUrl == null || dbUrl.isEmpty()) {
            throw new SQLException("DATABASE_URL environment variable is not set. Please add PostgreSQL database on Render.");
        }
        
        // Render's URL is in format: postgresql://user:pass@host:port/database
        // The PostgreSQL driver accepts this directly - no conversion needed
        System.out.println("Connecting to Render PostgreSQL at: " + dbUrl.substring(0, Math.min(50, dbUrl.length())) + "...");
        return DriverManager.getConnection(dbUrl);
    }
}