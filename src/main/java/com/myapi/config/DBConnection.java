package com.myapi.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // Use environment variables for Railway
    // private static final String DB_HOST = System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "mysql.railway.internal";
    // private static final String DB_PORT = System.getenv("DB_PORT") != null ? System.getenv("DB_PORT") : "3306";
    // private static final String DB_NAME = System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : "railway";
    private static final String DB_USER = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "root";
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "dBuFCYXSjiJFCUMCnXpcCpaADWcMHmzh";
    
    private static final String URL = "mysql://root:dBuFCYXSjiJFCUMCnXpcCpaADWcMHmzh@autorack.proxy.rlwy.net:18721/railway";
    
    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(URL, DB_USER, DB_PASSWORD);
    }
}