package com.snapdex.backend.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.snapdex.backend.entity.AttendanceRecord;
import com.snapdex.backend.entity.AttendanceSession;
import com.snapdex.backend.entity.Student;
import com.snapdex.backend.repository.AttendanceRecordRepository;
import com.snapdex.backend.repository.AttendanceSessionRepository;
import com.snapdex.backend.repository.StudentRepository;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "*")
public class AttendanceController {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final StudentRepository studentRepository;

    public AttendanceController(AttendanceSessionRepository sessionRepository,
                                AttendanceRecordRepository recordRepository,
                                StudentRepository studentRepository) {
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.studentRepository = studentRepository;
    }

    // =====================================================
    // SAVE FINALIZED ATTENDANCE
    // =====================================================
    @PostMapping
    public ResponseEntity<?> saveAttendance(@RequestBody Map<String, Object> payload) {
        try {
            AttendanceSession session = new AttendanceSession();

            if (payload.get("classId") != null) {
                session.setClassId(String.valueOf(payload.get("classId")));
            }
            if (payload.get("facultyId") != null) {
                session.setFacultyId(String.valueOf(payload.get("facultyId")));
            }
            if (payload.get("facultyName") != null) {
                session.setFacultyName(String.valueOf(payload.get("facultyName")));
            }
            if (payload.get("department") != null) {
                session.setDepartment(String.valueOf(payload.get("department")));
            }
            if (payload.get("program") != null) {
                session.setProgram(String.valueOf(payload.get("program")));
            }
            if (payload.get("year") != null) {
                session.setYear(String.valueOf(payload.get("year")));
            }
            if (payload.get("semester") != null) {
                session.setSemester(String.valueOf(payload.get("semester")));
            }
            if (payload.get("section") != null) {
                session.setSection(String.valueOf(payload.get("section")));
            }
            if (payload.get("subject") != null) {
                session.setSubject(String.valueOf(payload.get("subject")));
            } else {
                return ResponseEntity.badRequest().body("Subject is required");
            }

            if (session.getFacultyId() == null || session.getFacultyId().trim().isEmpty()) {
                session.setFacultyId("FAC-DEFAULT");
            }

            session.setStatus(payload.get("status") != null ? String.valueOf(payload.get("status")) : "FINALIZED");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawRecords = (List<Map<String, Object>>) payload.get("records");
            if (rawRecords == null) {
                rawRecords = new ArrayList<>();
            }

            int presentCount = 0;
            int absentCount = 0;

            for (Map<String, Object> rec : rawRecords) {
                String att = rec.get("attendance") != null ? String.valueOf(rec.get("attendance")).toUpperCase() : "REVIEW";
                if ("PRESENT".equals(att)) {
                    presentCount++;
                } else if ("ABSENT".equals(att)) {
                    absentCount++;
                }
            }

            session.setTotalStudents(rawRecords.size());
            session.setPresentCount(presentCount);
            session.setAbsentCount(absentCount);
            session.setDateTime(LocalDateTime.now());

            AttendanceSession savedSession = sessionRepository.save(session);

            List<AttendanceRecord> savedRecords = new ArrayList<>();
            for (Map<String, Object> rec : rawRecords) {
                String regNo = rec.get("registrationNumber") != null
                        ? String.valueOf(rec.get("registrationNumber"))
                        : (rec.get("studentId") != null ? String.valueOf(rec.get("studentId")) : "");

                String name = rec.get("name") != null
                        ? String.valueOf(rec.get("name"))
                        : (rec.get("studentName") != null ? String.valueOf(rec.get("studentName")) : "");

                String att = rec.get("attendance") != null
                        ? String.valueOf(rec.get("attendance")).toUpperCase()
                        : "REVIEW";

                String recState = rec.get("recognition") != null
                        ? String.valueOf(rec.get("recognition"))
                        : "MATCHED";

                AttendanceRecord record = new AttendanceRecord(
                        savedSession.getId(),
                        regNo,
                        name,
                        att,
                        recState
                );
                savedRecords.add(recordRepository.save(record));
            }

            Map<String, Object> response = buildSessionDto(savedSession, savedRecords);
            response.put("message", "Attendance saved successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to save attendance: " + e.getMessage());
        }
    }

    // =====================================================
    // GET ATTENDANCE HISTORY
    // =====================================================
    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory(
            @RequestParam(required = false) String facultyId,
            @RequestParam(required = false) String subject) {

        List<AttendanceSession> sessions;

        if (facultyId != null && !facultyId.trim().isEmpty() && subject != null && !subject.trim().isEmpty()) {
            sessions = sessionRepository.findByFacultyIdAndSubjectOrderByDateTimeDesc(facultyId.trim(), subject.trim());
        } else if (facultyId != null && !facultyId.trim().isEmpty()) {
            sessions = sessionRepository.findByFacultyIdOrderByDateTimeDesc(facultyId.trim());
        } else if (subject != null && !subject.trim().isEmpty()) {
            sessions = sessionRepository.findBySubjectOrderByDateTimeDesc(subject.trim());
        } else {
            sessions = sessionRepository.findAllByOrderByDateTimeDesc();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (AttendanceSession session : sessions) {
            List<AttendanceRecord> records = recordRepository.findBySessionId(session.getId());
            result.add(buildSessionDto(session, records));
        }

        return ResponseEntity.ok(result);
    }

    // =====================================================
    // GET SESSION DETAILS BY ID
    // =====================================================
    @GetMapping("/session/{id}")
    public ResponseEntity<?> getSessionById(@PathVariable Long id) {
        AttendanceSession session = sessionRepository.findById(id).orElse(null);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Attendance session not found");
        }
        List<AttendanceRecord> records = recordRepository.findBySessionId(session.getId());
        return ResponseEntity.ok(buildSessionDto(session, records));
    }

    // =====================================================
    // GET ATTENDANCE FOR STUDENT
    // =====================================================
    @GetMapping("/student/{registrationNumber}")
    public ResponseEntity<?> getStudentAttendance(@PathVariable String registrationNumber) {
        String regNo = registrationNumber.trim();
        Student student = studentRepository.findByRegistrationNumber(regNo).orElse(null);
        List<AttendanceRecord> records = recordRepository.findByRegistrationNumberOrderByIdDesc(regNo);

        long total = 0;
        long present = 0;
        long absent = 0;

        List<Map<String, Object>> historyList = new ArrayList<>();
        for (AttendanceRecord record : records) {
            AttendanceSession session = sessionRepository.findById(record.getSessionId()).orElse(null);
            if (session == null) continue;

            // Only FINALIZED attendance sessions count
            if (session.getStatus() == null || !session.getStatus().equalsIgnoreCase("FINALIZED")) {
                continue;
            }

            // Student must belong to corresponding department, program, year, semester, section
            if (student != null) {
                if (session.getDepartment() != null && student.getDepartment() != null
                        && !normalize(session.getDepartment()).equals(normalize(student.getDepartment()))) {
                    continue;
                }
                if (session.getProgram() != null && student.getProgram() != null
                        && !normalize(session.getProgram()).equals(normalize(student.getProgram()))) {
                    continue;
                }
                if (session.getYear() != null && student.getYear() != null
                        && !normalizeYear(session.getYear()).equals(normalizeYear(student.getYear()))) {
                    continue;
                }
                if (session.getSemester() != null && student.getSemester() != null
                        && !normalizeSemester(session.getSemester()).equals(normalizeSemester(student.getSemester()))) {
                    continue;
                }
                if (session.getSection() != null && student.getSection() != null
                        && !normalizeSection(session.getSection()).equals(normalizeSection(student.getSection()))) {
                    continue;
                }
            }

            total++;
            if ("PRESENT".equalsIgnoreCase(record.getAttendance())) {
                present++;
            } else if ("ABSENT".equalsIgnoreCase(record.getAttendance())) {
                absent++;
            }

            Map<String, Object> item = new HashMap<>();
            item.put("recordId", record.getId());
            item.put("sessionId", record.getSessionId());
            item.put("attendance", record.getAttendance());
            item.put("recognition", record.getRecognition());
            item.put("status", session.getStatus());
            item.put("dateTime", session.getDateTime());
            item.put("subject", session.getSubject());
            item.put("department", session.getDepartment());
            item.put("program", session.getProgram());
            item.put("year", session.getYear());
            item.put("semester", session.getSemester());
            item.put("section", session.getSection());
            item.put("facultyName", session.getFacultyName());
            historyList.add(item);
        }

        int percentage = total > 0 ? (int) Math.round(((double) present / total) * 100) : 0;

        Map<String, Object> response = new HashMap<>();
        response.put("registrationNumber", regNo);
        response.put("totalClasses", total);
        response.put("presentClasses", present);
        response.put("absentClasses", absent);
        response.put("percentage", percentage);
        response.put("history", historyList);

        return ResponseEntity.ok(response);
    }

    // =====================================================
    // GET REPORTS
    // =====================================================
    @GetMapping("/reports")
    public ResponseEntity<Map<String, Object>> getReports(@RequestParam(required = false) String facultyId) {
        List<AttendanceSession> sessions;
        if (facultyId != null && !facultyId.trim().isEmpty()) {
            sessions = sessionRepository.findByFacultyIdOrderByDateTimeDesc(facultyId.trim());
        } else {
            sessions = sessionRepository.findAllByOrderByDateTimeDesc();
        }

        int totalSessions = sessions.size();
        int totalPresent = 0;
        int totalAbsent = 0;
        int totalRecords = 0;

        // Group by Subject + Section
        Map<String, Map<String, Object>> subjectGroups = new HashMap<>();

        for (AttendanceSession session : sessions) {
            totalPresent += session.getPresentCount() != null ? session.getPresentCount() : 0;
            totalAbsent += session.getAbsentCount() != null ? session.getAbsentCount() : 0;
            totalRecords += session.getTotalStudents() != null ? session.getTotalStudents() : 0;

            String subKey = (session.getSubject() != null ? session.getSubject() : "General") + "|" +
                    (session.getSection() != null ? session.getSection() : "All");

            if (!subjectGroups.containsKey(subKey)) {
                Map<String, Object> grp = new HashMap<>();
                grp.put("subject", session.getSubject() != null ? session.getSubject() : "General");
                grp.put("section", session.getSection() != null ? session.getSection() : "All");
                grp.put("sessions", 0);
                grp.put("present", 0);
                grp.put("absent", 0);
                grp.put("studentSet", new HashSet<String>());
                subjectGroups.put(subKey, grp);
            }

            Map<String, Object> grp = subjectGroups.get(subKey);
            grp.put("sessions", (int) grp.get("sessions") + 1);
            grp.put("present", (int) grp.get("present") + (session.getPresentCount() != null ? session.getPresentCount() : 0));
            grp.put("absent", (int) grp.get("absent") + (session.getAbsentCount() != null ? session.getAbsentCount() : 0));

            @SuppressWarnings("unchecked")
            Set<String> set = (Set<String>) grp.get("studentSet");
            List<AttendanceRecord> recs = recordRepository.findBySessionId(session.getId());
            for (AttendanceRecord r : recs) {
                set.add(r.getRegistrationNumber());
            }
        }

        int totalAtt = totalPresent + totalAbsent;
        int overallPercentage = totalAtt > 0 ? (int) Math.round(((double) totalPresent / totalAtt) * 100) : 0;

        List<Map<String, Object>> subjectList = new ArrayList<>();
        for (Map<String, Object> grp : subjectGroups.values()) {
            @SuppressWarnings("unchecked")
            Set<String> set = (Set<String>) grp.get("studentSet");
            int p = (int) grp.get("present");
            int a = (int) grp.get("absent");
            int tot = p + a;
            int pct = tot > 0 ? (int) Math.round(((double) p / tot) * 100) : 0;

            Map<String, Object> subMap = new HashMap<>();
            subMap.put("subject", grp.get("subject"));
            subMap.put("section", grp.get("section"));
            subMap.put("sessions", grp.get("sessions"));
            subMap.put("totalStudents", set.size());
            subMap.put("present", p);
            subMap.put("absent", a);
            subMap.put("percentage", pct);
            subjectList.add(subMap);
        }

        Map<String, Object> report = new HashMap<>();
        report.put("totalSessions", totalSessions);
        report.put("totalRecords", totalRecords);
        report.put("totalPresent", totalPresent);
        report.put("totalAbsent", totalAbsent);
        report.put("overallPercentage", overallPercentage);
        report.put("subjects", subjectList);

        return ResponseEntity.ok(report);
    }

    private Map<String, Object> buildSessionDto(AttendanceSession session, List<AttendanceRecord> records) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", session.getId());
        dto.put("classId", session.getClassId());
        dto.put("facultyId", session.getFacultyId());
        dto.put("facultyName", session.getFacultyName());
        dto.put("department", session.getDepartment());
        dto.put("program", session.getProgram());
        dto.put("year", session.getYear());
        dto.put("semester", session.getSemester());
        dto.put("section", session.getSection());
        dto.put("subject", session.getSubject());
        dto.put("dateTime", session.getDateTime());
        dto.put("totalStudents", session.getTotalStudents());
        dto.put("present", session.getPresentCount());
        dto.put("absent", session.getAbsentCount());
        dto.put("status", session.getStatus());
        dto.put("capturedPhotos", session.getCapturedPhotos());
        dto.put("createdAt", session.getCreatedAt());

        List<Map<String, Object>> recordsList = new ArrayList<>();
        for (AttendanceRecord r : records) {
            Map<String, Object> rMap = new HashMap<>();
            rMap.put("id", r.getId());
            rMap.put("registrationNumber", r.getRegistrationNumber());
            rMap.put("studentId", r.getRegistrationNumber());
            rMap.put("name", r.getStudentName());
            rMap.put("studentName", r.getStudentName());
            rMap.put("attendance", r.getAttendance());
            rMap.put("recognition", r.getRecognition());
            recordsList.add(rMap);
        }
        dto.put("records", recordsList);

        return dto;
    }

    private String normalize(String val) {
        return val == null ? "" : val.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private String normalizeYear(String val) {
        if (val == null) return "";
        String s = val.trim().toLowerCase();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(s);
        return m.find() ? m.group() : s.replaceAll("(?i)\\s*year\\s*", "").trim();
    }

    private String normalizeSemester(String val) {
        if (val == null) return "";
        String s = val.trim().toLowerCase();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(s);
        return m.find() ? m.group() : s.replaceAll("(?i)semester\\s*", "").trim();
    }

    private String normalizeSection(String val) {
        if (val == null) return "";
        return val.trim().toUpperCase().replaceAll("(?i)SECTION\\s*", "").trim();
    }
}
