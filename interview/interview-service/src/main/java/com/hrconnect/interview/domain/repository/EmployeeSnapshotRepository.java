package com.hrconnect.interview.domain.repository;

import com.hrconnect.interview.domain.model.EmployeeSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeSnapshotRepository extends JpaRepository<EmployeeSnapshot, Long> {

    Optional<EmployeeSnapshot> findByEmployeeId(String employeeId);

    boolean existsByEmployeeId(String employeeId);
}
