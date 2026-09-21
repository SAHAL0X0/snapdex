package com.snapdex.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.snapdex.backend.entity.Faculty;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {

    Optional<Faculty> findByEmail(String email);

    Optional<Faculty> findByFacultyId(String facultyId);

    boolean existsByEmail(String email);

    boolean existsByFacultyId(String facultyId);
}