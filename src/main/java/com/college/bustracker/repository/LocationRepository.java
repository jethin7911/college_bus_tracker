package com.college.bustracker.repository;

import com.college.bustracker.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    List<Location> findByAssignmentIdOrderByTimestampDesc(Long assignmentId);

    @Query("SELECT l FROM Location l WHERE l.assignment.id = :assignmentId ORDER BY l.timestamp DESC LIMIT 1")
    Optional<Location> findLatestByAssignmentId(Long assignmentId);

    List<Location> findByAssignmentIdAndTimestampBetween(Long assignmentId, LocalDateTime start, LocalDateTime end);

    // Deletes old location records — used by the hourly cleanup in LocationService
    @Modifying
    @Transactional
    @Query("DELETE FROM Location l WHERE l.timestamp < :cutoff")
    void deleteByTimestampBefore(LocalDateTime cutoff);
}