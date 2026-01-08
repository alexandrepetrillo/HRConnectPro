package com.hrconnect.leave.application.service;

import com.hrconnect.leave.domain.model.Leave;
import com.hrconnect.leave.domain.repository.EmployeeSnapshotRepository;
import com.hrconnect.leave.domain.repository.LeaveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service métier pour la gestion des congés
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveService {

    private final LeaveRepository leaveRepository;
    private final EmployeeSnapshotRepository employeeSnapshotRepository;

    /**
     * Crée un nouveau congé
     */
    @Transactional
    public Leave createLeave(Leave leave) {
        log.info("Creating leave for employee: {}", leave.getEmployeeId());

        // TODO: Vérifier que l'employé existe dans le snapshot local
        // TODO: Valider les dates
        // TODO: Calculer le nombre de jours posés

        Leave savedLeave = leaveRepository.save(leave);
        log.info("Leave created with id: {}", savedLeave.getId());

        // TODO: Publier l'événement leave.state

        return savedLeave;
    }

    /**
     * Récupère tous les congés d'un employé
     */
    @Transactional(readOnly = true)
    public List<Leave> getLeavesByEmployeeId(String employeeId) {
        log.debug("Fetching leaves for employee: {}", employeeId);
        return leaveRepository.findByEmployeeId(employeeId);
    }

    /**
     * Récupère tous les congés
     */
    @Transactional(readOnly = true)
    public List<Leave> getAllLeaves() {
        log.debug("Fetching all leaves");
        return leaveRepository.findAll();
    }

    /**
     * Récupère un congé par son ID
     */
    @Transactional(readOnly = true)
    public Leave getLeaveById(Long id) {
        log.debug("Fetching leave with id: {}", id);
        return leaveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave not found with id: " + id));
    }

    /**
     * Met à jour un congé
     */
    @Transactional
    public Leave updateLeave(Long id, Leave leaveDetails) {
        log.info("Updating leave with id: {}", id);

        Leave leave = getLeaveById(id);

        // TODO: Mettre à jour les champs
        // TODO: Publier l'événement leave.state

        return leaveRepository.save(leave);
    }

    /**
     * Supprime un congé
     */
    @Transactional
    public void deleteLeave(Long id) {
        log.info("Deleting leave with id: {}", id);
        leaveRepository.deleteById(id);
    }
}

