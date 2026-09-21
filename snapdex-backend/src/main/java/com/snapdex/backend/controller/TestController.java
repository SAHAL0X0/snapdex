package com.snapdex.backend.controller;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TestController {

    @Autowired(required = false)
    private DataSource dataSource;

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
}