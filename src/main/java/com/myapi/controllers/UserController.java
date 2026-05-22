package com.myapi.controllers;

import com.myapi.config.DBConnection;
import com.myapi.utils.JWTUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

@WebServlet("/api/users/*")
public class UserController extends HttpServlet {
    
    private Gson gson = new Gson();
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        
        if (pathInfo == null || "/".equals(pathInfo)) {
            getAllUsers(request, response);
        } else {
            response.setStatus(404);
            response.getWriter().write("{\"errors\":true,\"message\":\"Endpoint not found\"}");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        
        if ("/login".equals(pathInfo)) {
            login(request, response);
        } else if ("/register".equals(pathInfo) || "/".equals(pathInfo)) {
            registerUser(request, response);
        } else {
            response.setStatus(404);
            response.getWriter().write("{\"errors\":true,\"message\":\"Endpoint not found\"}");
        }
    }
    
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && !"/".equals(pathInfo)) {
            updateUser(request, response);
        } else {
            response.setStatus(404);
            response.getWriter().write("{\"errors\":true,\"message\":\"Endpoint not found\"}");
        }
    }
    
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && !"/".equals(pathInfo)) {
            deleteUser(request, response);
        } else {
            response.setStatus(404);
            response.getWriter().write("{\"errors\":true,\"message\":\"Endpoint not found\"}");
        }
    }
    
    // GET /api/users - Get all users
    private void getAllUsers(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        try (Connection conn = DBConnection.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, name, email, created_at FROM users");
            
            JsonArray users = new JsonArray();
            while (rs.next()) {
                JsonObject user = new JsonObject();
                user.addProperty("id", rs.getInt("id"));
                user.addProperty("name", rs.getString("name"));
                user.addProperty("email", rs.getString("email"));
                user.addProperty("created_at", rs.getString("created_at"));
                users.add(user);
            }
            
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("errors", false);
            responseJson.add("data", users);
            out.print(gson.toJson(responseJson));
            
        } catch (Exception e) {
            response.setStatus(500);
            JsonObject error = new JsonObject();
            error.addProperty("errors", true);
            error.addProperty("message", e.getMessage());
            out.print(gson.toJson(error));
        }
    }
    
    // POST /api/users/register - Register new user
    private void registerUser(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        
        if (name == null || email == null || password == null) {
            response.setStatus(400);
            JsonObject error = new JsonObject();
            error.addProperty("errors", true);
            error.addProperty("message", "Name, email, and password are required");
            out.print(gson.toJson(error));
            return;
        }
        
        try (Connection conn = DBConnection.getConnection()) {
            // Check if user exists
            String checkSql = "SELECT id FROM users WHERE email = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, email);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                response.setStatus(400);
                JsonObject error = new JsonObject();
                error.addProperty("errors", true);
                error.addProperty("message", "User already exists with this email");
                out.print(gson.toJson(error));
                return;
            }
            
            // Hash password
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            
            // Insert user
            String sql = "INSERT INTO users (name, email, password) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, hashedPassword);
            
            int affected = pstmt.executeUpdate();
            
            if (affected > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int userId = generatedKeys.getInt(1);
                    
                    JsonObject userData = new JsonObject();
                    userData.addProperty("id", userId);
                    userData.addProperty("name", name);
                    userData.addProperty("email", email);
                    
                    JsonObject result = new JsonObject();
                    result.addProperty("errors", false);
                    result.add("data", userData);
                    out.print(gson.toJson(result));
                }
            } else {
                response.setStatus(500);
                JsonObject error = new JsonObject();
                error.addProperty("errors", true);
                error.addProperty("message", "Failed to register user");
                out.print(gson.toJson(error));
            }
            
        } catch (Exception e) {
            response.setStatus(500);
            JsonObject error = new JsonObject();
            error.addProperty("errors", true);
            error.addProperty("message", e.getMessage());
            out.print(gson.toJson(error));
        }
    }
    
    // POST /api/users/login - Login user
    private void login(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        
        if (email == null || password == null) {
            response.setStatus(400);
            JsonObject error = new JsonObject();
            error.addProperty("errors", true);
            error.addProperty("message", "Email and password are required");
            out.print(gson.toJson(error));
            return;
        }
        
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM users WHERE email = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String hashedPassword = rs.getString("password");
                
                if (BCrypt.checkpw(password, hashedPassword)) {
                    int userId = rs.getInt("id");
                    String token = JWTUtil.generateToken(email, userId);
                    
                    JsonObject userData = new JsonObject();
                    userData.addProperty("id", userId);
                    userData.addProperty("name", rs.getString("name"));
                    userData.addProperty("email", rs.getString("email"));
                    
                    JsonObject data = new JsonObject();
                    data.addProperty("token", token);
                    data.add("user", userData);
                    
                    JsonObject result = new JsonObject();
                    result.addProperty("errors", false);
                    result.add("data", data);
                    out.print(gson.toJson(result));
                } else {
                    response.setStatus(401);
                    JsonObject error = new JsonObject();
                    error.addProperty("errors", true);
                    error.addProperty("message", "Invalid email or password");
                    out.print(gson.toJson(error));
                }
            } else {
                response.setStatus(401);
                JsonObject error = new JsonObject();
                error.addProperty("errors", true);
                error.addProperty("message", "Invalid email or password");
                out.print(gson.toJson(error));
            }
            
        } catch (Exception e) {
            response.setStatus(500);
            JsonObject error = new JsonObject();
            error.addProperty("errors", true);
            error.addProperty("message", e.getMessage());
            out.print(gson.toJson(error));
        }
    }
    
    // PUT /api/users/{id} - Update user
    private void updateUser(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        String pathInfo = request.getPathInfo();
        int userId = Integer.parseInt(pathInfo.substring(1));
        
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        
        try (Connection conn = DBConnection.getConnection()) {
            StringBuilder sql = new StringBuilder("UPDATE users SET ");
            boolean hasUpdate = false;
            
            if (name != null && !name.isEmpty()) {
                sql.append("name = ?");
                hasUpdate = true;
            }
            if (email != null && !email.isEmpty()) {
                if (hasUpdate) sql.append(", ");
                sql.append("email = ?");
                hasUpdate = true;
            }
            
            if (!hasUpdate) {
                response.setStatus(400);
                JsonObject error = new JsonObject();
                error.addProperty("errors", true);
                error.addProperty("message", "No fields to update");
                out.print(gson.toJson(error));
                return;
            }
            
            sql.append(" WHERE id = ?");
            
            PreparedStatement pstmt = conn.prepareStatement(sql.toString());
            int index = 1;
            if (name != null && !name.isEmpty()) {
                pstmt.setString(index++, name);
            }
            if (email != null && !email.isEmpty()) {
                pstmt.setString(index++, email);
            }
            pstmt.setInt(index, userId);
            
            pstmt.executeUpdate();
            
            JsonObject result = new JsonObject();
            result.addProperty("errors", false);
            result.addProperty("message", "User updated successfully");
            out.print(gson.toJson(result));
            
        } catch (Exception e) {
            response.setStatus(500);
            JsonObject error = new JsonObject();
            error.addProperty("errors", true);
            error.addProperty("message", e.getMessage());
            out.print(gson.toJson(error));
        }
    }
    
    // DELETE /api/users/{id} - Delete user
    private void deleteUser(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        String pathInfo = request.getPathInfo();
        int userId = Integer.parseInt(pathInfo.substring(1));
        
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "DELETE FROM users WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
            
            JsonObject result = new JsonObject();
            result.addProperty("errors", false);
            result.addProperty("message", "User deleted successfully");
            out.print(gson.toJson(result));
            
        } catch (Exception e) {
            response.setStatus(500);
            JsonObject error = new JsonObject();
            error.addProperty("errors", true);
            error.addProperty("message", e.getMessage());
            out.print(gson.toJson(error));
        }
    }
}