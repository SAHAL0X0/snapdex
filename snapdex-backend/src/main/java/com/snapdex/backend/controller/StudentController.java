package com.snapdex.backend.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.snapdex.backend.entity.ClassEntity;
import com.snapdex.backend.entity.Student;
import com.snapdex.backend.repository.ClassRepository;
import com.snapdex.backend.repository.StudentRepository;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public StudentController(StudentRepository studentRepository, ClassRepository classRepository) {
        this.studentRepository = studentRepository;
        this.classRepository = classRepository;
    }

    // =====================================================
    // STUDENT REGISTRATION
    // =====================================================
    @PostMapping("/register")
    public ResponseEntity<?> registerStudent(@RequestBody Student student) {
        if (student.getName() == null || student.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Full Name is required");
        }

        if (student.getRegistrationNumber() == null || student.getRegistrationNumber().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Registration Number is required");
        }

        if (student.getEmail() == null || student.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Email is required");
        }

        String email = student.getEmail().trim().toLowerCase();
        String regNo = student.getRegistrationNumber().trim();

        if (studentRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body("This email is already registered");
        }

        if (studentRepository.existsByRegistrationNumber(regNo)) {
            return ResponseEntity.badRequest().body("This Registration Number is already registered");
        }

        if (student.getPassword() == null || student.getPassword().length() < 8) {
            return ResponseEntity.badRequest().body("Password must be at least 8 characters");
        }

        student.setEmail(email);
        student.setRegistrationNumber(regNo);
        student.setName(student.getName().trim());

        // BCrypt password hashing
        student.setPassword(passwordEncoder.encode(student.getPassword()));

        Student savedStudent = studentRepository.save(student);

        Map<String, Object> response = getSafeStudentMap(savedStudent);
        response.put("message", "Student registered successfully");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =====================================================
    // STUDENT LOGIN
    // =====================================================
    @PostMapping("/login")
    public ResponseEntity<?> loginStudent(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body("Email and password are required");
        }

        email = email.trim().toLowerCase();

        Student student = studentRepository.findByEmail(email).orElse(null);
        if (student == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Student profile not found with this email");
        }

        boolean passwordMatches = passwordEncoder.matches(password, student.getPassword());
        if (!passwordMatches) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Incorrect password");
        }

        Map<String, Object> response = getSafeStudentMap(student);
        response.put("message", "Login successful");

        return ResponseEntity.ok(response);
    }

    // =====================================================
    // GET STUDENTS BY CLASS ID
    // =====================================================
    @GetMapping("/class/{classId}")
    public ResponseEntity<?> getStudentsByClass(@PathVariable Long classId) {
        ClassEntity classEntity = classRepository.findById(classId).orElse(null);
        if (classEntity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Class not found");
        }

        List<Student> allStudents = studentRepository.findAll();
        List<Map<String, Object>> matchedStudents = allStudents.stream()
                .filter(s -> matchesClass(s, classEntity))
                .map(this::getSafeStudentMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(matchedStudents);
    }

    // =====================================================
    // GET STUDENTS (CLASS OR ACADEMIC SCOPED)
    // =====================================================
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getStudents(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String program,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String section) {

        if (classId != null) {
            ClassEntity classEntity = classRepository.findById(classId).orElse(null);
            if (classEntity == null) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            List<Map<String, Object>> safeStudents = studentRepository.findAll().stream()
                    .filter(s -> matchesClass(s, classEntity))
                    .map(this::getSafeStudentMap)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(safeStudents);
        }

        boolean hasAcademicParams = hasText(department) && hasText(program)
                && hasText(year) && hasText(semester) && hasText(section);

        if (hasAcademicParams) {
            List<Map<String, Object>> safeStudents = studentRepository.findAll().stream()
                    .filter(s -> matchesAcademic(s, department, program, year, semester, section))
                    .map(this::getSafeStudentMap)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(safeStudents);
        }

        // If parameters are missing or incomplete:
        // DO NOT return all students. Return an empty list [] to prevent cross-class leakage.
        return ResponseEntity.ok(Collections.emptyList());
    }

    private boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
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

    private boolean matchesAcademic(Student s, String dept, String prog, String yr, String sem, String sec) {
        if (dept != null && !dept.trim().isEmpty() && !normalize(dept).equals(normalize(s.getDepartment()))) {
            return false;
        }
        if (prog != null && !prog.trim().isEmpty() && !normalize(prog).equals(normalize(s.getProgram()))) {
            return false;
        }
        if (yr != null && !yr.trim().isEmpty() && !normalizeYear(yr).equals(normalizeYear(s.getYear()))) {
            return false;
        }
        if (sem != null && !sem.trim().isEmpty() && !normalizeSemester(sem).equals(normalizeSemester(s.getSemester()))) {
            return false;
        }
        if (sec != null && !sec.trim().isEmpty() && !normalizeSection(sec).equals(normalizeSection(s.getSection()))) {
            return false;
        }
        return true;
    }

    private boolean matchesClass(Student s, ClassEntity c) {
        return matchesAcademic(s, c.getDepartment(), c.getProgram(), c.getYear(), c.getSemester(), c.getSection());
    }

    // =====================================================
    // GET STUDENT BY REGISTRATION NUMBER
    // =====================================================
    @GetMapping("/{registrationNumber}")
    public ResponseEntity<?> getStudentByRegNo(@PathVariable String registrationNumber) {
        Student student = studentRepository.findByRegistrationNumber(registrationNumber.trim()).orElse(null);
        if (student == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Student not found");
        }
        return ResponseEntity.ok(getSafeStudentMap(student));
    }

    // Helper to safely strip sensitive credentials
    private Map<String, Object> getSafeStudentMap(Student student) {
        Map<String, Object> safe = new HashMap<>();
        safe.put("id", student.getId());
        safe.put("registrationNumber", student.getRegistrationNumber());
        safe.put("studentId", student.getRegistrationNumber());
        safe.put("name", student.getName());
        safe.put("email", student.getEmail());
        safe.put("department", student.getDepartment());
        safe.put("program", student.getProgram());
        safe.put("year", student.getYear());
        safe.put("semester", student.getSemester());
        safe.put("section", student.getSection());
        safe.put("profilePhoto", student.getProfilePhoto());
        safe.put("createdAt", student.getCreatedAt());
        return safe;
    }
}
