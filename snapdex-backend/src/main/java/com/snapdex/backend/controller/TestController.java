package com.snapdex.backend.controller;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.snapdex.backend.entity.ClassEntity;
import com.snapdex.backend.entity.Faculty;
import com.snapdex.backend.entity.Student;
import com.snapdex.backend.repository.ClassRepository;
import com.snapdex.backend.repository.FacultyRepository;
import com.snapdex.backend.repository.StudentRepository;
import com.snapdex.backend.service.DatabaseCleanupService;

@RestController
@RequestMapping("/api")
public class TestController {

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private DatabaseCleanupService databaseCleanupService;

    @Autowired(required = false)
    private ClassRepository classRepository;

    @Autowired(required = false)
    private StudentRepository studentRepository;

    @Autowired(required = false)
    private FacultyRepository facultyRepository;

    @GetMapping("/test")
    public String test() {
        return "SnapDex Backend is Working!";
    }

    @GetMapping("/test/db")
    public Map<String, Object> testDb() {
        Map<String, Object> res = new HashMap<>();
        if (dataSource == null) {
            res.put("status", "ERROR");
            res.put("message", "DataSource bean is null");
            return res;
        }

        try (Connection conn = dataSource.getConnection()) {
            res.put("status", "CONNECTED");
            res.put("url", conn.getMetaData().getURL());
            res.put("catalog", conn.getCatalog());
            res.put("user", conn.getMetaData().getUserName());

            List<String> tables = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SHOW TABLES;")) {
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
            }
            res.put("tables", tables);
        } catch (Exception e) {
            res.put("status", "ERROR");
            res.put("error", e.getClass().getName() + ": " + e.getMessage());
        }
        return res;
    }

    @GetMapping("/test/db-inspect")
    public Map<String, Object> testDbInspect() {
        Map<String, Object> res = new HashMap<>();

        if (classRepository != null) {
            List<Map<String, Object>> classes = classRepository.findAll().stream().map(c -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", c.getId());
                m.put("facultyId", c.getFacultyId());
                m.put("department", c.getDepartment());
                m.put("program", c.getProgram());
                m.put("year", c.getYear());
                m.put("semester", c.getSemester());
                m.put("section", c.getSection());
                m.put("subject", c.getSubject());
                return m;
            }).collect(Collectors.toList());
            res.put("classes", classes);
            res.put("totalClasses", classes.size());
        }

        if (studentRepository != null) {
            List<Map<String, Object>> students = studentRepository.findAll().stream().map(s -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", s.getId());
                m.put("name", s.getName());
                m.put("registrationNumber", s.getRegistrationNumber());
                m.put("email", s.getEmail());
                m.put("department", s.getDepartment());
                m.put("section", s.getSection());
                return m;
            }).collect(Collectors.toList());
            res.put("students", students);
            res.put("totalStudents", students.size());
        }

        if (facultyRepository != null) {
            List<Map<String, Object>> faculties = facultyRepository.findAll().stream().map(f -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", f.getId());
                m.put("name", f.getName());
                m.put("email", f.getEmail());
                m.put("facultyId", f.getFacultyId());
                return m;
            }).collect(Collectors.toList());
            res.put("faculties", faculties);
            res.put("totalFaculties", faculties.size());
        }

        return res;
    }

    @GetMapping("/test/db-cleanup")
    public Map<String, Object> testDbCleanupGet() {
        if (databaseCleanupService != null) {
            return databaseCleanupService.performCleanup();
        }
        Map<String, Object> res = new HashMap<>();
        res.put("error", "DatabaseCleanupService bean is null");
        return res;
    }

    @PostMapping("/test/db-cleanup")
    public Map<String, Object> testDbCleanupPost() {
        return testDbCleanupGet();
    }

    @GetMapping("/test/env")
    public Map<String, Object> testEnv() {
        Map<String, Object> map = new HashMap<>();
        map.put("MYSQLHOST", System.getenv("MYSQLHOST"));
        map.put("MYSQLPORT", System.getenv("MYSQLPORT"));
        map.put("MYSQLDATABASE", System.getenv("MYSQLDATABASE"));
        map.put("MYSQLUSER", System.getenv("MYSQLUSER"));
        map.put("hasPassword", System.getenv("MYSQLPASSWORD") != null && !System.getenv("MYSQLPASSWORD").isEmpty());
        map.put("hasMysqlUrl", System.getenv("MYSQL_URL") != null && !System.getenv("MYSQL_URL").isEmpty());
        map.put("hasDatabaseUrl", System.getenv("DATABASE_URL") != null && !System.getenv("DATABASE_URL").isEmpty());
        return map;
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(Throwable.class)
    public Map<String, Object> handleTestException(Throwable t) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", t.getClass().getName());
        error.put("message", t.getMessage());
        error.put("cause", t.getCause() != null ? t.getCause().getMessage() : null);
        return error;
    }
}