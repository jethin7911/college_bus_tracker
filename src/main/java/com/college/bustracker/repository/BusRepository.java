package com.college.bustracker.repository;

import com.college.bustracker.entity.Bus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusRepository extends JpaRepository<Bus, Long> {

    Optional<Bus> findByBusName(String busName);

    boolean existsByBusName(String busName);
}