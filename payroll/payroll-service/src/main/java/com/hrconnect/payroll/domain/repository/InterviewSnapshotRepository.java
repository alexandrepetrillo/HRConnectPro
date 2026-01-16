package com.hrconnect.payroll.domain.repository;

import com.hrconnect.payroll.domain.model.InterviewSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InterviewSnapshotRepository extends JpaRepository<InterviewSnapshot, String> {

    List<InterviewSnapshot> findByEmployeeId(String employeeId);

    /**
     * Récupère toutes les augmentations validées pour un employé
     */
    @Query("SELECT i FROM InterviewSnapshot i WHERE i.employeeId = :employeeId " +
           "AND i.statut = 'VALIDE' AND i.augmentationAccordee IS NOT NULL AND i.augmentationAccordee > 0")
    List<InterviewSnapshot> findValidatedAugmentations(@Param("employeeId") String employeeId);

    /**
     * Récupère les augmentations validées jusqu'à une date donnée (pour calcul rétroactif)
     */
    @Query("SELECT i FROM InterviewSnapshot i WHERE i.employeeId = :employeeId " +
           "AND i.statut = 'VALIDE' AND i.augmentationAccordee IS NOT NULL AND i.augmentationAccordee > 0 " +
           "AND i.dateEntretien <= :upToDate")
    List<InterviewSnapshot> findValidatedAugmentationsUpToDate(
            @Param("employeeId") String employeeId,
            @Param("upToDate") LocalDate upToDate
    );
}
