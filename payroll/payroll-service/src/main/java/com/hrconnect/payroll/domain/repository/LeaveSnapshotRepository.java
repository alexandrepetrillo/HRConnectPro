package com.hrconnect.payroll.domain.repository;

import com.hrconnect.payroll.domain.model.LeaveSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveSnapshotRepository extends JpaRepository<LeaveSnapshot, String> {

    List<LeaveSnapshot> findByEmployeeId(String employeeId);

    /**
     * Récupère les congés sans solde approuvés pour un employé sur une période donnée
     */
    @Query("SELECT l FROM LeaveSnapshot l WHERE l.employeeId = :employeeId " +
           "AND l.type = 'SANS_SOLDE' AND l.statut = 'APPROUVE' " +
           "AND l.dateDebut <= :periodEnd AND l.dateFin >= :periodStart")
    List<LeaveSnapshot> findCongesSansSoldeForPeriod(
            @Param("employeeId") String employeeId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );

    /**
     * Récupère tous les congés d'un employé sur une période donnée
     */
    @Query("SELECT l FROM LeaveSnapshot l WHERE l.employeeId = :employeeId " +
           "AND l.dateDebut <= :periodEnd AND l.dateFin >= :periodStart")
    List<LeaveSnapshot> findByEmployeeIdAndPeriod(
            @Param("employeeId") String employeeId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );
}
