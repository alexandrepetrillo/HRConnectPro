package com.hrconnect.payroll.domain.repository;

import com.hrconnect.payroll.domain.model.EmployeeSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeSnapshotRepository extends JpaRepository<EmployeeSnapshot, String> {

    Optional<EmployeeSnapshot> findByEmployeeId(String employeeId);

    Optional<EmployeeSnapshot> findByReference(String reference);

    List<EmployeeSnapshot> findByDepartement(String departement);

    boolean existsByEmployeeId(String employeeId);
}
