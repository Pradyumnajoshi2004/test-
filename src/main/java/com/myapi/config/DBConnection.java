package com.myapi.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    
    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        
        // Try to get MySQL URL from Railway
        String mysqlUrl = System.getenv("MYSQL_URL");
        
        if (mysqlUrl != null && !mysqlUrl.isEmpty()) {
            // Railway provides: mysql://root:password@mysql.railway.internal:3306/railway
            // Convert to JDBC format: jdbc:mysql://root:password@mysql.railway.internal:3306/railway
            String jdbcUrl = mysqlUrl.replace("mysql://", "jdbc:mysql://");
            return DriverManager.getConnection(jdbcUrl);
        }
        
        // Fallback for local development
        String host = System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "localhost";
        String port = System.getenv("DB_PORT") != null ? System.getenv("DB_PORT") : "3306";
        String database = System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : "railway";
        String user = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "root";
        String password = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "dBuFCYXSjiJFCUMCnXpcCpaADWcMHmzh";
        
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + 
                     "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        
        return DriverManager.getConnection(url, user, password);
    }
}