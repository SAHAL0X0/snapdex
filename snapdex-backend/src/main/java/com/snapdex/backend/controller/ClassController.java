package com.snapdex.backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.snapdex.backend.entity.ClassEntity;
import com.snapdex.backend.entity.Faculty;
import com.snapdex.backend.repository.ClassRepository;
import com.snapdex.backend.repository.FacultyRepository;

@RestController
@RequestMapping("/api/classes")
@CrossOrigin(origins = "*")
public class ClassController {

    private final ClassRepository classRepository;
    private final FacultyRepository facultyRepository;

    public ClassController(ClassRepository classRepository, FacultyRepository facultyRepository) {
        this.classRepository = classRepository;
        this.facultyRepository = facultyRepository;
    }

    // =====================================================
    // GET CLASSES BY FACULTY ID
    // =====================================================
    @GetMapping("/faculty/{facultyId}")
    public ResponseEntity<?> getClassesByFaculty(@PathVariable String facultyId) {
        if (facultyId == null || facultyId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Faculty ID is required");
        }

        String trimmedId = facultyId.trim();
        List<ClassEntity> classes = classRepository.findByFacultyIdOrderByIdDesc(trimmedId);

        // If not found by given ID, and ID might be the numeric primary key, check faculty table
        if (classes.isEmpty()) {
            try {
                Long numericId = Long.parseLong(trimmedId);
                Faculty faculty = facultyRepository.findById(numericId).orElse(null);
                if (faculty != null && faculty.getFacultyId() != null && !faculty.getFacultyId().equals(trimmedId)) {
                    classes = classRepository.findByFacultyIdOrderByIdDesc(faculty.getFacultyId());
                }
            } catch (NumberFormatException ignored) {
                // Also check if trimmedId is faculty_id string (e.g. I0097) and faculty has numeric ID
                Faculty faculty = facultyRepository.findByFacultyId(trimmedId).orElse(null);
                if (faculty != null && faculty.getId() != null) {
                    classes = classRepository.findByFacultyIdOrderByIdDesc(String.valueOf(faculty.getId()));
                }
            }
        }

        return ResponseEntity.ok(classes);
    }

    // =====================================================
    // GET ALL CLASSES
    // =====================================================
    @GetMapping
    public ResponseEntity<List<ClassEntity>> getAllClasses(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String program,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String section) {

        if (department != null && program != null && year != null && semester != null && section != null) {
            return ResponseEntity.ok(classRepository.findByDepartmentAndProgramAndYearAndSemesterAndSection(
                    department, program, year, semester, section
            ));
        }

        return ResponseEntity.ok(classRepository.findAll());
    }

    // =====================================================
    // GET CLASS BY ID
    // =====================================================
    @GetMapping("/{id}")
    public ResponseEntity<?> getClassById(@PathVariable Long id) {
        return classRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // =====================================================
    // CREATE NEW CLASS
    // =====================================================
    @PostMapping
    public ResponseEntity<?> createClass(@RequestBody ClassEntity newClass) {
        if (newClass.getFacultyId() == null || newClass.getFacultyId().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Faculty ID is required");
        }
        if (newClass.getDepartment() == null || newClass.getDepartment().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Department is required");
        }
        if (newClass.getProgram() == null || newClass.getProgram().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Program is required");
        }
        if (newClass.getYear() == null || newClass.getYear().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Year is required");
        }
        if (newClass.getSemester() == null || newClass.getSemester().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Semester is required");
        }
        if (newClass.getSection() == null || newClass.getSection().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Section is required");
        }
        if (newClass.getSubject() == null || newClass.getSubject().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Subject is required");
        }

        String facultyId = newClass.getFacultyId().trim();
        String department = newClass.getDepartment().trim();
        String program = newClass.getProgram().trim();
        String year = newClass.getYear().trim();
        String semester = newClass.getSemester().trim();
        String section = newClass.getSection().trim();
        String subject = newClass.getSubject().trim();

        // Check for duplicate class record
        boolean exists = classRepository.existsByFacultyIdAndDepartmentAndProgramAndYearAndSemesterAndSectionAndSubject(
                facultyId, department, program, year, semester, section, subject
        );

        if (exists) {
            return ResponseEntity.badRequest().body("This class and subject is already added for this faculty");
        }

        newClass.setFacultyId(facultyId);
        newClass.setDepartment(department);
        newClass.setProgram(program);
        newClass.setYear(year);
        newClass.setSemester(semester);
        newClass.setSection(section);
        newClass.setSubject(subject);

        ClassEntity savedClass = classRepository.save(newClass);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedClass);
    }

    // =====================================================
    // DELETE CLASS
    // =====================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteClass(
            @PathVariable Long id,
            @RequestParam(required = false) String facultyId) {

        ClassEntity classEntity = classRepository.findById(id).orElse(null);
        if (classEntity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Class not found");
        }

        // Security check: only the owning faculty can delete this class
        if (facultyId != null && !facultyId.trim().isEmpty()) {
            String incomingFacultyId = facultyId.trim();
            boolean isOwner = classEntity.getFacultyId().equals(incomingFacultyId);

            if (!isOwner) {
                // If ID is numeric or string, resolve with faculty table
                try {
                    Long numId = Long.parseLong(incomingFacultyId);
                    Faculty faculty = facultyRepository.findById(numId).orElse(null);
                    if (faculty != null && faculty.getFacultyId() != null && faculty.getFacultyId().equals(classEntity.getFacultyId())) {
                        isOwner = true;
                    }
                } catch (NumberFormatException ignored) {
                    Faculty faculty = facultyRepository.findByFacultyId(incomingFacultyId).orElse(null);
                    if (faculty != null && String.valueOf(faculty.getId()).equals(classEntity.getFacultyId())) {
                        isOwner = true;
                    }
                }
            }

            if (!isOwner) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You are not authorized to delete this class");
            }
        }

        classRepository.delete(classEntity);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Class deleted successfully");
        response.put("id", id);
        return ResponseEntity.ok(response);
    }
}
