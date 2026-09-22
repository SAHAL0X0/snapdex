package com.snapdex.backend.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.snapdex.backend.entity.AttendanceRecord;
import com.snapdex.backend.entity.AttendanceSession;
import com.snapdex.backend.entity.ClassEntity;
import com.snapdex.backend.entity.Faculty;
import com.snapdex.backend.entity.Student;
import com.snapdex.backend.repository.AttendanceRecordRepository;
import com.snapdex.backend.repository.AttendanceSessionRepository;
import com.snapdex.backend.repository.ClassRepository;
import com.snapdex.backend.repository.FacultyRepository;
import com.snapdex.backend.repository.StudentRepository;

@Service
public class DatabaseCleanupService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseCleanupService.class);

    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final ClassRepository classRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;

    public DatabaseCleanupService(
            StudentRepository studentRepository,
            FacultyRepository facultyRepository,
            ClassRepository classRepository,
            AttendanceSessionRepository sessionRepository,
            AttendanceRecordRepository recordRepository) {
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.classRepository = classRepository;
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        try {
            log.info("Running automatic database cleanup on startup...");
            Map<String, Object> result = performCleanup();
            log.info("Startup database cleanup completed: {}", result);
        } catch (Exception e) {
            log.error("Startup database cleanup failed: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public Map<String, Object> performCleanup() {
        Map<String, Object> report = new HashMap<>();

        // 1. Clean dummy test students
        List<Student> allStudents = studentRepository.findAll();
        List<Student> dummyStudents = new ArrayList<>();
        for (Student s : allStudents) {
            String name = s.getName() != null ? s.getName().toLowerCase() : "";
            String email = s.getEmail() != null ? s.getEmail().toLowerCase() : "";
            String reg = s.getRegistrationNumber() != null ? s.getRegistrationNumber().toUpperCase() : "";

            if (name.contains("student test") || email.startsWith("student.test") || reg.startsWith("RA1790")) {
                dummyStudents.add(s);
            }
        }

        List<String> removedStudentNames = new ArrayList<>();
        for (Student s : dummyStudents) {
            removedStudentNames.add(s.getName() + " (" + s.getRegistrationNumber() + ")");
            studentRepository.delete(s);
        }
        report.put("deletedDummyStudentsCount", dummyStudents.size());
        report.put("deletedDummyStudents", removedStudentNames);

        // 2. Clean dummy test faculties
        List<Faculty> allFaculties = facultyRepository.findAll();
        List<Faculty> dummyFaculties = new ArrayList<>();
        for (Faculty f : allFaculties) {
            String name = f.getName() != null ? f.getName().toLowerCase() : "";
            String email = f.getEmail() != null ? f.getEmail().toLowerCase() : "";
            if (name.contains("prof. automated") || email.startsWith("prof.test")) {
                dummyFaculties.add(f);
            }
        }
        List<String> removedFacultyNames = new ArrayList<>();
        for (Faculty f : dummyFaculties) {
            removedFacultyNames.add(f.getName() + " (" + f.getEmail() + ")");
            facultyRepository.delete(f);
        }
        report.put("deletedDummyFacultiesCount", dummyFaculties.size());
        report.put("deletedDummyFaculties", removedFacultyNames);

        // 3. Clean attendance records of dummy students
        List<AttendanceRecord> allRecords = recordRepository.findAll();
        int deletedRecordsCount = 0;
        for (AttendanceRecord rec : allRecords) {
            String name = rec.getStudentName() != null ? rec.getStudentName().toLowerCase() : "";
            String reg = rec.getRegistrationNumber() != null ? rec.getRegistrationNumber().toUpperCase() : "";
            if (name.contains("student test") || reg.startsWith("RA1790")) {
                recordRepository.delete(rec);
                deletedRecordsCount++;
            }
        }
        report.put("deletedAttendanceRecordsCount", deletedRecordsCount);

        // 4. Deduplicate classes
        List<ClassEntity> allClasses = classRepository.findAll();
        Map<String, ClassEntity> primaryClassMap = new LinkedHashMap<>();
        List<ClassEntity> duplicateClasses = new ArrayList<>();

        for (ClassEntity cls : allClasses) {
            String key = (normalize(cls.getDepartment()) + "|"
                    + normalize(cls.getProgram()) + "|"
                    + normalize(cls.getYear()) + "|"
                    + normalize(cls.getSemester()) + "|"
                    + normalize(cls.getSection()) + "|"
                    + normalize(cls.getSubject())).toLowerCase();

            if (!primaryClassMap.containsKey(key)) {
                primaryClassMap.put(key, cls);
            } else {
                duplicateClasses.add(cls);
            }
        }

        List<String> removedClassNames = new ArrayList<>();
        for (ClassEntity dup : duplicateClasses) {
            String key = (normalize(dup.getDepartment()) + "|"
                    + normalize(dup.getProgram()) + "|"
                    + normalize(dup.getYear()) + "|"
                    + normalize(dup.getSemester()) + "|"
                    + normalize(dup.getSection()) + "|"
                    + normalize(dup.getSubject())).toLowerCase();
            ClassEntity canonical = primaryClassMap.get(key);

            // Re-point any attendance sessions referencing dup.getId() to canonical.getId()
            if (canonical != null) {
                List<AttendanceSession> sessions = sessionRepository.findAll();
                for (AttendanceSession session : sessions) {
                    if (String.valueOf(dup.getId()).equals(session.getClassId())) {
                        session.setClassId(String.valueOf(canonical.getId()));
                        sessionRepository.save(session);
                    }
                }
            }

            removedClassNames.add("ID: " + dup.getId() + " - " + dup.getSubject() + " (Sec " + dup.getSection() + ")");
            classRepository.delete(dup);
        }

        report.put("deletedDuplicateClassesCount", duplicateClasses.size());
        report.put("deletedDuplicateClasses", removedClassNames);

        return report;
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim();
    }
}
