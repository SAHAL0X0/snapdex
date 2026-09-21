package com.snapdex.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "attendance_records")
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "registration_number", nullable = false)
    private String registrationNumber;

    @Column(name = "student_name")
    private String studentName;

    @Column(nullable = false)
    private String attendance; // "PRESENT", "ABSENT", "REVIEW"

    @Column
    private String recognition; // "MATCHED", "REVIEW"

    public AttendanceRecord() {
    }

    public AttendanceRecord(Long sessionId, String registrationNumber, String studentName, String attendance, String recognition) {
        this.sessionId = sessionId;
        this.registrationNumber = registrationNumber;
        this.studentName = studentName;
        this.attendance = attendance;
        this.recognition = recognition;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getAttendance() {
        return attendance;
    }

    public void setAttendance(String attendance) {
        this.attendance = attendance;
    }

    public String getRecognition() {
        return recognition;
    }

    public void setRecognition(String recognition) {
        this.recognition = recognition;
    }
}
