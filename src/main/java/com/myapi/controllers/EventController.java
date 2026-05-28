package com.myapi.controllers;

import com.myapi.config.DBConnection;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

@WebServlet("/api/events/*")
public class EventController extends HttpServlet {
    
    private Gson gson = new Gson();
    
    // Helper method to read JSON from request body
    private JsonObject getJsonFromRequest(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return JsonParser.parseString(sb.toString()).getAsJsonObject();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        
        if (pathInfo == null || "/".equals(pathInfo)) {
            getAllEvents(request, response);
        } else {
            getEventById(request, response);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        createEvent(request, response);
    }
    
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        updateEvent(request, response);
    }
    
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        deleteEvent(request, response);
    }
    
    // GET /api/events - Get all events with user names
    private void getAllEvents(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT e.*, u.name as user_name, u.email as user_email FROM events e " +
                        "LEFT JOIN users u ON e.user_id = u.id " +
                        "ORDER BY e.date DESC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            JsonArray events = new JsonArray();
            while (rs.next()) {
                JsonObject event = new JsonObject();
                event.addProperty("id", rs.getInt("id"));
                event.addProperty("eventName", rs.getString("event_name"));
                event.addProperty("eventDescription", rs.getString("event_description"));
                event.addProperty("eventVenue", rs.getString("event_venue"));
                event.addProperty("date", rs.getString("date"));
                event.addProperty("userId", rs.getInt("user_id"));
                event.addProperty("userName", rs.getString("user_name"));
                event.addProperty("userEmail", rs.getString("user_email"));
                event.addProperty("created_at", rs.getString("created_at"));
                events.add(event);
            }
            
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("errors", false);
            responseJson.add("data", events);
            out.print(gson.toJson(responseJson));
            
        } catch (Exception e) {
            response.setStatus(500);
            JsonObject error = new JsonObject();
            error.addProperty("errors", true);
            error.addProperty("message", e.getMessage());
            out.print(gson.toJson(error));
        }
    }
    
    // GET /api/events/{id} - Get event by ID
    private void getEventById(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        String pathInfo = request.getPathInfo();
        int eventId = Integer.parseInt(pathInfo.substring(1));
        
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT e.*, u.name as user_name, u.email as user_email FROM events e " +
                        "LEFT JOIN users u ON e.user_id = u.id " +
                        "WHERE e.id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, eventId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                JsonObject event = new JsonObject();
                event.addProperty("id", rs.getInt("id"));
                event.addProperty("eventName", rs.getString("event_name"));
                event.addProperty("eventDescription", rs.getString("event_description"));
                event.addProperty("eventVenue", rs.getString("event_venue"));
                event.addProperty("date", rs.getString("date"));
                event.addProperty("userId", rs.getInt("user_id"));
                event.addProperty("userName", rs.getString("user_name"));
                event.addProperty("userEmail", rs.getString("user_email"));
                event.addProperty("created_at", rs.getString("created_at"));
                
                JsonObject responseJson = new JsonObject();
                responseJson.addProperty("errors", false);
                responseJson.add("data", event);
                out.print(gson.toJson(responseJson));
            } else {
                response.setStatus(404);
                JsonObject error = new JsonObject();
                error.addProperty("errors", true);
                error.addProperty("message", "Event not found");
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
    
    // POST /api/events - Create new event
    private void createEvent(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            response.setStatus(401);
            JsonObject error = new JsonObject();
            error.addProperty("errors", true);
            error.addProperty("message", "User not authenticated");
            out.print(gson.toJson(error));
            return;
        }
        
        try {
            JsonObject json = getJsonFromRequest(request);
            
            String eventName = json.has("eventName") ? json.get("eventName").getAsString() : null;
            String eventDescription = json.has("eventDescription") ? json.get("eventDescription").getAsString() : null;
            String eventVenue = json.has("eventVenue") ? json.get("eventVenue").getAsString() : null;
            String dateStr = json.has("date") ? json.get("date").getAsString() : null;
            
            if (eventName == null || eventVenue == null || dateStr == null) {
                response.setStatus(400);
                JsonObject error = new JsonObject();
                error.addProperty("errors", true);
                error.addProperty("message", "eventName, eventVenue, and date are required");
                out.print(gson.toJson(error));
                return;
            }
            
            try (Connection conn = DBConnection.getConnection()) {
                String checkSql = "SELECT id FROM events WHERE event_name = ? AND date = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setString(1, eventName);
                checkStmt.setString(2, dateStr);
                ResultSet rs = checkStmt.executeQuery();
                
                if (rs.next()) {
                    response.setStatus(400);
                    JsonObject error = new JsonObject();
                    error.addProperty("errors", true);
                    error.addProperty("message", "Event already exists on this date");
                    out.print(gson.toJson(error));
                    return;
                }
                
                String sql = "INSERT INTO events (event_name, event_description, event_venue, date, user_id) " +
                            "VALUES (?, ?, ?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                pstmt.setString(1, eventName);
                pstmt.setString(2, eventDescription);
                pstmt.setString(3, eventVenue);
                pstmt.setDate(4, Date.valueOf(dateStr));
                pstmt.setInt(5, userId);
                
                int affected = pstmt.executeUpdate();
                
                if (affected > 0) {
                    ResultSet generatedKeys = pstmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        int eventId = generatedKeys.getInt(1);
                        
                        JsonObject eventData = new JsonObject();
                        eventData.addProperty("id", eventId);
                        eventData.addProperty("eventName", eventName);
                        eventData.addProperty("eventDescription", eventDescription);
                        eventData.addProperty("eventVenue", eventVenue);
                        eventData.addProperty("date", dateStr);
                        eventData.addProperty("userId", userId);
                        
                        JsonObject result = new JsonObject();
                        result.addProperty("errors", false);
                        result.add("data", eventData);
                        out.print(gson.toJson(result));
                    }
                } else {
                    response.setStatus(500);
                    JsonObject error = new JsonObject();
                    error.addProperty("errors", true);
                    error.addProperty("message", "Failed to create event");
                    out.print(gson.toJson(error));
                }
            }
        } catch (Exception e) {
            response.setStatus(500);
            JsonObject error = new JsonObject();
            error.addProperty("errors", true);
            error.addProperty("message", e.getMessage());
            out.print(gson.toJson(error));
        }
    }
    
    // PUT /api/events/{id} - Update event (only creator can update)
    private void updateEvent(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        String pathInfo = request.getPathInfo();
        int eventId = Integer.parseInt(pathInfo.substring(1));
        
        Integer loggedInUserId = (Integer) request.getAttribute("userId");
        if (loggedInUserId == null) {
            response.setStatus(401);
            JsonObject error = new JsonObject();
            error.addProperty("errors", true);
            error.addProperty("message", "User not authenticated");
            out.print(gson.toJson(error));
            return;
        }
        
        try {
            JsonObject json = getJsonFromRequest(request);
            
            String eventName = json.has("eventName") ? json.get("eventName").getAsString() : null;
            String eventDescription = json.has("eventDescription") ? json.get("eventDescription").getAsString() : null;
            String eventVenue = json.has("eventVenue") ? json.get("eventVenue").getAsString() : null;
            String dateStr = json.has("date") ? json.get("date").getAsString() : null;
            
            try (Connection conn = DBConnection.getConnection()) {
                String checkSql = "SELECT user_id FROM events WHERE id = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setInt(1, eventId);
                ResultSet rs = checkStmt.executeQuery();
                
                if (!rs.next()) {
                    response.setStatus(404);
                    JsonObject error = new JsonObject();
                    error.addProperty("errors", true);
                    error.addProperty("message", "Event not found");
                    out.print(gson.toJson(error));
                    return;
                }
                
                int eventCreatorId = rs.getInt("user_id");
                
                if (loggedInUserId != eventCreatorId) {
                    response.setStatus(403);
                    JsonObject error = new JsonObject();
                    error.addProperty("errors", true);
                    error.addProperty("message", "You don't have permission to update this event");
                    out.print(gson.toJson(error));
                    return;
                }
                
                StringBuilder sql = new StringBuilder("UPDATE events SET ");
                boolean hasUpdate = false;
                
                if (eventName != null && !eventName.isEmpty()) {
                    sql.append("event_name = ?");
                    hasUpdate = true;
                }
                if (eventDescription != null && !eventDescription.isEmpty()) {
                    if (hasUpdate) sql.append(", ");
                    sql.append("event_description = ?");
                    hasUpdate = true;
                }
                if (eventVenue != null && !eventVenue.isEmpty()) {
                    if (hasUpdate) sql.append(", ");
                    sql.append("event_venue = ?");
                    hasUpdate = true;
                }
                if (dateStr != null && !dateStr.isEmpty()) {
                    if (hasUpdate) sql.append(", ");
                    sql.append("date = ?");
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
                if (eventName != null && !eventName.isEmpty()) {
                    pstmt.setString(index++, eventName);
                }
                if (eventDescription != null && !eventDescription.isEmpty()) {
                    pstmt.setString(index++, eventDescription);
                }
                if (eventVenue != null && !eventVenue.isEmpty()) {
                    pstmt.setString(index++, eventVenue);
                }
                if (dateStr != null && !dateStr.isEmpty()) {
                    pstmt.setDate(index++, Date.valueOf(dateStr));
                }
                pstmt.setInt(index, eventId);
                
                pstmt.executeUpdate();
                
                JsonObject result = new JsonObject();
                result.addProperty("errors", false);
                result.addProperty("message", "Event updated successfully");
                out.print(gson.toJson(result));
            }
        } catch (Exception e) {
            response.setStatus(500);
            JsonObject error = new JsonObject();
            error.addProperty("errors", true);
            error.addProperty("message", e.getMessage());
            out.print(gson.toJson(error));
        }
    }
    
    // DELETE /api/events/{id} - Delete event (only creator can delete)
    private void deleteEvent(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        String pathInfo = request.getPathInfo();
        int eventId = Integer.parseInt(pathInfo.substring(1));
        
        Integer loggedInUserId = (Integer) request.getAttribute("userId");
        if (loggedInUserId == null) {
            response.setStatus(401);
            JsonObject error = new JsonObject();
            error.addProperty("errors", true);
            error.addProperty("message", "User not authenticated");
            out.print(gson.toJson(error));
            return;
        }
        
        try (Connection conn = DBConnection.getConnection()) {
            String checkSql = "SELECT user_id FROM events WHERE id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, eventId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (!rs.next()) {
                response.setStatus(404);
                JsonObject error = new JsonObject();
                error.addProperty("errors", true);
                error.addProperty("message", "Event not found");
                out.print(gson.toJson(error));
                return;
            }
            
            int eventCreatorId = rs.getInt("user_id");
            
            if (loggedInUserId != eventCreatorId) {
                response.setStatus(403);
                JsonObject error = new JsonObject();
                error.addProperty("errors", true);
                error.addProperty("message", "You don't have permission to delete this event");
                out.print(gson.toJson(error));
                return;
            }
            
            String sql = "DELETE FROM events WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, eventId);
            pstmt.executeUpdate();
            
            JsonObject result = new JsonObject();
            result.addProperty("errors", false);
            result.addProperty("message", "Event deleted successfully");
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