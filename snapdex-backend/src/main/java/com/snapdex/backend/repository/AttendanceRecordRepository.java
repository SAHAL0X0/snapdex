package com.snapdex.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.snapdex.backend.entity.AttendanceRecord;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    List<AttendanceRecord> findBySessionId(Long sessionId);

    List<AttendanceRecord> findByRegistrationNumberOrderByIdDesc(String registrationNumber);

    long countByRegistrationNumberAndAttendance(String registrationNumber, String attendance);

    long countByRegistrationNumber(String registrationNumber);
}
