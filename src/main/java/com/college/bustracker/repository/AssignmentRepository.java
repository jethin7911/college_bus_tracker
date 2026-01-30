package com.college.bustracker.repository;

import com.college.bustracker.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    Optional<Assignment> findByBusIdAndIsActiveTrue(Long busId);

    Optional<Assignment> findByDriverIdAndIsActiveTrue(Long driverId);

    List<Assignment> findByIsActiveTrueOrderByStartedAtDesc();

    @Query("SELECT a FROM Assignment a WHERE a.isActive = true AND a.startedAt < :timeout")
    List<Assignment> findStaleAssignments(LocalDateTime timeout);
}