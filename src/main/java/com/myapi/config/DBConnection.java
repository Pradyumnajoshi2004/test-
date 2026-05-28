package com.myapi.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    
    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        // Load PostgreSQL driver
        Class.forName("org.postgresql.Driver");
        
        // Get DATABASE_URL from Render environment variable
        String dbUrl = System.getenv("DATABASE_URL");
        
        if (dbUrl == null || dbUrl.isEmpty()) {
            // Fallback for local development (if you have PostgreSQL installed)
            String host = "localhost";
            String port = "5432";
            String database = "event_api_db";
            String user = "postgres";
            String password = "postgres";
            dbUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database;
        }
        
        System.out.println("Connecting to database...");
        return DriverManager.getConnection(dbUrl);
    }
}