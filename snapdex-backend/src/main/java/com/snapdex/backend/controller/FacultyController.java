package com.snapdex.backend.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.snapdex.backend.entity.Faculty;
import com.snapdex.backend.repository.FacultyRepository;

@RestController
@RequestMapping("/api/faculty")
@CrossOrigin(origins = "*")
public class FacultyController {

    private final FacultyRepository facultyRepository;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public FacultyController(FacultyRepository facultyRepository) {
        this.facultyRepository = facultyRepository;
    }

    /**
     * Automatically generates the next unique Faculty ID in sequence:
     * FAC-1001, FAC-1002, FAC-1003, etc.
     */
    private synchronized String generateNextFacultyId() {
        List<Faculty> allFaculties = facultyRepository.findAll();
        int maxId = 1000;
        for (Faculty f : allFaculties) {
            String fId = f.getFacultyId();
            if (fId != null && fId.toUpperCase().startsWith("FAC-")) {
                try {
                    int num = Integer.parseInt(fId.substring(4).trim());
                    if (num > maxId) {
                        maxId = num;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        int nextId = maxId + 1;
        String generated = "FAC-" + nextId;
        while (facultyRepository.existsByFacultyId(generated)) {
            nextId++;
            generated = "FAC-" + nextId;
        }
        return generated;
    }


    // =====================================================
    // FACULTY REGISTER (AUTO-GENERATED FACULTY ID & OPTIONAL PHOTO)
    // =====================================================

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Faculty faculty) {

        if (faculty.getName() == null ||
                faculty.getName().trim().isEmpty()) {

            return ResponseEntity.badRequest()
                    .body("Name is required");
        }

        if (faculty.getEmail() == null ||
                !faculty.getEmail()
                        .trim()
                        .toLowerCase()
                        .endsWith("@srmist.edu.in")) {

            return ResponseEntity.badRequest()
                    .body("Please use your official SRM email");
        }

        if (faculty.getPassword() == null ||
                faculty.getPassword().length() < 8) {

            return ResponseEntity.badRequest()
                    .body("Password must be at least 8 characters");
        }

        String email = faculty.getEmail()
                .trim()
                .toLowerCase();

        if (facultyRepository.existsByEmail(email)) {

            return ResponseEntity.badRequest()
                    .body("Email already registered");
        }

        faculty.setEmail(email);

        // Auto-generate unique Faculty ID (e.g. FAC-1001, FAC-1002, ...)
        String generatedFacultyId = generateNextFacultyId();
        faculty.setFacultyId(generatedFacultyId);

        // BCrypt password hashing
        faculty.setPassword(
                passwordEncoder.encode(faculty.getPassword())
        );

        Faculty savedFaculty =
                facultyRepository.save(faculty);

        Map<String, Object> response =
                new HashMap<>();

        response.put("message",
                "Faculty registered successfully");

        response.put("id",
                savedFaculty.getId());

        response.put("name",
                savedFaculty.getName());

        response.put("email",
                savedFaculty.getEmail());

        // Return auto-generated unique Faculty ID
        response.put("facultyId",
                savedFaculty.getFacultyId());

        response.put("profilePhoto",
                savedFaculty.getProfilePhoto());

        response.put("department",
                savedFaculty.getDepartment());

        return ResponseEntity.ok(response);
    }


    // =====================================================
    // UPDATE PROFILE PHOTO (OPTIONAL PHOTO MANAGEMENT)
    // =====================================================

    @PutMapping("/{id}/photo")
    public ResponseEntity<?> updateProfilePhoto(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {

        String newPhoto = request.get("profilePhoto");

        Faculty faculty = null;
        try {
            Long numericId = Long.parseLong(id);
            faculty = facultyRepository.findById(numericId).orElse(null);
        } catch (NumberFormatException ignored) {}

        if (faculty == null) {
            faculty = facultyRepository.findByFacultyId(id).orElse(null);
        }

        if (faculty == null) {
            return ResponseEntity.status(404).body("Faculty not found");
        }

        faculty.setProfilePhoto(newPhoto);
        Faculty saved = facultyRepository.save(faculty);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Profile photo updated successfully");
        response.put("facultyId", saved.getFacultyId());
        response.put("profilePhoto", saved.getProfilePhoto());
        return ResponseEntity.ok(response);
    }


    // =====================================================
    // FACULTY LOGIN
    // =====================================================

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody Faculty loginData) {

        if (loginData.getEmail() == null ||
                loginData.getPassword() == null) {

            return ResponseEntity.badRequest()
                    .body("Email and password are required");
        }

        String email = loginData.getEmail()
                .trim()
                .toLowerCase();

        Faculty faculty = facultyRepository
                .findByEmail(email)
                .orElse(null);

        if (faculty == null) {

            return ResponseEntity.status(401)
                    .body("Invalid email or password");
        }

        boolean passwordMatches =
                passwordEncoder.matches(
                        loginData.getPassword(),
                        faculty.getPassword()
                );

        if (!passwordMatches) {

            return ResponseEntity.status(401)
                    .body("Invalid email or password");
        }

        Map<String, Object> response =
                new HashMap<>();

        response.put("message",
                "Login successful");

        response.put("id",
                faculty.getId());

        response.put("name",
                faculty.getName());

        response.put("email",
                faculty.getEmail());

        response.put("facultyId",
                faculty.getFacultyId());

        response.put("profilePhoto",
                faculty.getProfilePhoto());

        response.put("department",
                faculty.getDepartment());

        return ResponseEntity.ok(response);
    }


    // =====================================================
    // FORGOT PASSWORD - GENERATE OTP
    // =====================================================

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");

        if (email == null ||
                email.trim().isEmpty()) {

            return ResponseEntity.badRequest()
                    .body("Email is required");
        }

        email = email.trim().toLowerCase();

        if (!email.endsWith("@srmist.edu.in")) {

            return ResponseEntity.badRequest()
                    .body("Please use your official SRM email");
        }

        Faculty faculty = facultyRepository
                .findByEmail(email)
                .orElse(null);

        if (faculty == null) {

            return ResponseEntity.badRequest()
                    .body("No faculty account found with this email");
        }

        // Generate 6 digit OTP
        String otp = String.format(
                "%06d",
                new Random().nextInt(1000000)
        );

        // OTP valid for 5 minutes
        LocalDateTime expiry =
                LocalDateTime.now().plusMinutes(5);

        faculty.setResetOtp(otp);
        faculty.setResetOtpExpiry(expiry);

        facultyRepository.save(faculty);

        /*
         * DEVELOPMENT MODE
         *
         * Later this OTP will be sent to the
         * official SRM email using SMTP.
         */
        System.out.println(
                "======================================"
        );

        System.out.println(
                "SnapDex Password Reset OTP"
        );

        System.out.println(
                "Email: " + email
        );

        System.out.println(
                "OTP: " + otp
        );

        System.out.println(
                "Valid for: 5 minutes"
        );

        System.out.println(
                "======================================"
        );

        return ResponseEntity.ok(
                "OTP generated successfully. " +
                "For development, check the backend console."
        );
    }


    // =====================================================
    // RESET PASSWORD
    // =====================================================

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");

        if (email == null ||
                otp == null ||
                newPassword == null) {

            return ResponseEntity.badRequest()
                    .body(
                        "Email, OTP and new password are required"
                    );
        }

        email = email.trim().toLowerCase();
        otp = otp.trim();

        if (!email.endsWith("@srmist.edu.in")) {

            return ResponseEntity.badRequest()
                    .body("Please use your official SRM email");
        }

        if (newPassword.length() < 8) {

            return ResponseEntity.badRequest()
                    .body(
                        "Password must be at least 8 characters"
                    );
        }

        Faculty faculty = facultyRepository
                .findByEmail(email)
                .orElse(null);

        if (faculty == null) {

            return ResponseEntity.badRequest()
                    .body("Faculty account not found");
        }

        // Check OTP
        if (faculty.getResetOtp() == null ||
                !faculty.getResetOtp().equals(otp)) {

            return ResponseEntity.badRequest()
                    .body("Invalid OTP");
        }

        // Check OTP expiry
        if (faculty.getResetOtpExpiry() == null ||
                LocalDateTime.now()
                        .isAfter(faculty.getResetOtpExpiry())) {

            faculty.setResetOtp(null);
            faculty.setResetOtpExpiry(null);

            facultyRepository.save(faculty);

            return ResponseEntity.badRequest()
                    .body("OTP has expired. Please request a new OTP");
        }

        // Hash new password
        faculty.setPassword(
                passwordEncoder.encode(newPassword)
        );

        // Clear OTP after successful reset
        faculty.setResetOtp(null);
        faculty.setResetOtpExpiry(null);

        facultyRepository.save(faculty);

        return ResponseEntity.ok(
                "Password reset successfully"
        );
    }
}