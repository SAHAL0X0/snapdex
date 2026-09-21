package com.snapdex.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.snapdex.backend.entity.AttendanceSession;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {

    List<AttendanceSession> findAllByOrderByDateTimeDesc();

    List<AttendanceSession> findByFacultyIdOrderByDateTimeDesc(String facultyId);

    List<AttendanceSession> findByFacultyIdAndSubjectOrderByDateTimeDesc(String facultyId, String subject);

    List<AttendanceSession> findBySubjectOrderByDateTimeDesc(String subject);
}
