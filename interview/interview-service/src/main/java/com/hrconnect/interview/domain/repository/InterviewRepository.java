package com.hrconnect.interview.domain.repository;

import com.hrconnect.interview.domain.model.Interview;
import com.hrconnect.interview.domain.model.InterviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {

    Optional<Interview> findByReference(String reference);

    List<Interview> findByEmployeeId(String employeeId);

    List<Interview> findByEmployeeIdAndStatut(String employeeId, InterviewStatus statut);

    List<Interview> findByStatut(InterviewStatus statut);
}
