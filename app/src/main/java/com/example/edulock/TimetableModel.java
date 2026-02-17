package com.example.edulock;

import java.io.Serializable;

public class TimetableModel implements Serializable {

    public String id;
    public String subject;
    public String notes;
    public String day;
    public String startTime;
    public String endTime;

    public TimetableModel() {
    }

    public TimetableModel(String id, String subject, String notes, String day, String startTime, String endTime) {
        this.id = id;
        this.subject = subject;
        this.notes = notes;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getId() { return id; }
    public String getSubject() { return subject; }
    public String getNotes() { return notes; }
    public String getDay() { return day; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }

    public void setId(String id) { this.id = id; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setDay(String day) { this.day = day; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
