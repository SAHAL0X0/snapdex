package com.snapdex.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.snapdex.backend.entity.ClassEntity;

@Repository
public interface ClassRepository extends JpaRepository<ClassEntity, Long> {

    List<ClassEntity> findByFacultyId(String facultyId);

    List<ClassEntity> findByFacultyIdOrderByIdDesc(String facultyId);

    Optional<ClassEntity> findByIdAndFacultyId(Long id, String facultyId);

    boolean existsByFacultyIdAndDepartmentAndProgramAndYearAndSemesterAndSectionAndSubject(
            String facultyId,
            String department,
            String program,
            String year,
            String semester,
            String section,
            String subject
    );

    List<ClassEntity> findByDepartmentAndProgramAndYearAndSemesterAndSection(
            String department,
            String program,
            String year,
            String semester,
            String section
    );
}
