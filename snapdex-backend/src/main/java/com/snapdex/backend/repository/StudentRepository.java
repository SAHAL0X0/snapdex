package com.snapdex.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.snapdex.backend.entity.Student;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEmail(String email);

    Optional<Student> findByRegistrationNumber(String registrationNumber);

    boolean existsByEmail(String email);

    boolean existsByRegistrationNumber(String registrationNumber);

    List<Student> findByDepartmentAndProgramAndYearAndSemesterAndSection(
            String department,
            String program,
            String year,
            String semester,
            String section
    );

    List<Student> findBySection(String section);
}
