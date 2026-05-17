package com.myapi.models;

import java.sql.Date;
import java.sql.Timestamp;

public class Event {
    private int id;
    private String eventName;
    private String eventDescription;
    private String eventVenue;
    private Date date;
    private int userId;
    private Timestamp createdAt;
    
    // Constructors
    public Event() {}
    
    public Event(String eventName, String eventDescription, String eventVenue, Date date, int userId) {
        this.eventName = eventName;
        this.eventDescription = eventDescription;
        this.eventVenue = eventVenue;
        this.date = date;
        this.userId = userId;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    
    public String getEventDescription() { return eventDescription; }
    public void setEventDescription(String eventDescription) { this.eventDescription = eventDescription; }
    
    public String getEventVenue() { return eventVenue; }
    public void setEventVenue(String eventVenue) { this.eventVenue = eventVenue; }
    
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}