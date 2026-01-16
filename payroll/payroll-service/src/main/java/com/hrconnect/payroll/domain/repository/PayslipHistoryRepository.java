package com.hrconnect.payroll.domain.repository;

import com.hrconnect.payroll.domain.model.PayslipHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayslipHistoryRepository extends JpaRepository<PayslipHistory, Long> {

    List<PayslipHistory> findByEmployeeIdOrderByPeriodMonthDesc(String employeeId);

    Optional<PayslipHistory> findByEmployeeIdAndPeriodMonth(String employeeId, String periodMonth);

    List<PayslipHistory> findByPeriodMonth(String periodMonth);

    boolean existsByEmployeeIdAndPeriodMonth(String employeeId, String periodMonth);
}
