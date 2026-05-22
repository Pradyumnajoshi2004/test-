package com.myapi.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    
    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("org.postgresql.Driver");
        
        // For Render PostgreSQL
        String dbUrl = System.getenv("DATABASE_URL");
        
        if (dbUrl != null && !dbUrl.isEmpty()) {
            // Render provides DATABASE_URL in format: postgresql://user:pass@host:port/db
            System.out.println("Connecting to Render PostgreSQL");
            return DriverManager.getConnection(dbUrl);
        }
        
        // Fallback for local development (if you have PostgreSQL locally)
        String host = "localhost";
        String port = "5432";
        String database = "event_api_di0q";
        String user = "event_api_di0q_user";
        String password = "0RhPJfMG5f45y98Lc5PW5LZyRKmrlaw3";
        
        String url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
        return DriverManager.getConnection(url, user, password);
    }
}