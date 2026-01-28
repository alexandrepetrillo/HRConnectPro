package com.hrconnect.leave.application.service;

import com.hrconnect.leave.application.dto.InitializeLeaveBalanceRequest;
import com.hrconnect.leave.application.dto.LeaveBalanceResponse;
import com.hrconnect.leave.domain.model.EmployeeLeaveBalance;
import com.hrconnect.leave.domain.model.Leave;
import com.hrconnect.leave.domain.repository.EmployeeLeaveBalanceRepository;
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
    private final EmployeeLeaveBalanceRepository employeeLeaveBalanceRepository;

    /**
     * Crée un nouveau congé
     */
    @Transactional
    public Leave createLeave(Leave leave) {
        log.info("Creating leave for employee: {}", leave.getEmployeeId());

        // Vérifier que les compteurs de congés existent pour cet employé
        EmployeeLeaveBalance balance = employeeLeaveBalanceRepository.findByEmployeeId(leave.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Employee leave balance not found. Please initialize balance first for employee: " + leave.getEmployeeId()));

        // TODO: Valider les dates
        // TODO: Calculer le nombre de jours posés
        // TODO: Vérifier que le solde est suffisant

        Leave savedLeave = leaveRepository.save(leave);
        log.info("Leave created with id: {}", savedLeave.getId());


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

    /**
     * Initialise les compteurs de congés pour un nouvel employé
     */
    @Transactional
    public LeaveBalanceResponse initializeLeaveBalance(String employeeId) {
        log.info("Initializing leave balance for employee: {}", employeeId);

        // Vérifier si les compteurs existent déjà
        if (employeeLeaveBalanceRepository.existsByEmployeeId(employeeId)) {
            log.warn("Leave balance already exists for employee: {}", employeeId);
            throw new IllegalArgumentException("Leave balance already exists for employee: " + employeeId);
        }

        // Valeurs par défaut si non spécifiées
        Integer cpAnnuels =  25;
        Integer rttAnnuels =  10;

        // Créer les compteurs
        EmployeeLeaveBalance balance = EmployeeLeaveBalance.builder()
                .employeeId(employeeId)
                .cpAnnuels(cpAnnuels)
                .rttAnnuels(rttAnnuels)
                .cpRestants(cpAnnuels)
                .rttRestants(rttAnnuels)
                .build();

        EmployeeLeaveBalance saved = employeeLeaveBalanceRepository.save(balance);
        log.info("Leave balance initialized for employee: {} (CP: {}, RTT: {})",
                employeeId, cpAnnuels, rttAnnuels);

        return LeaveBalanceResponse.builder()
                .employeeId(saved.getEmployeeId())
                .cpAnnuels(saved.getCpAnnuels())
                .rttAnnuels(saved.getRttAnnuels())
                .cpRestants(saved.getCpRestants())
                .rttRestants(saved.getRttRestants())
                .build();
    }

    /**
     * Récupère les compteurs de congés d'un employé
     */
    @Transactional(readOnly = true)
    public LeaveBalanceResponse getLeaveBalance(String employeeId) {
        log.debug("Fetching leave balance for employee: {}", employeeId);

        EmployeeLeaveBalance balance = employeeLeaveBalanceRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new RuntimeException("Leave balance not found for employee: " + employeeId));

        return LeaveBalanceResponse.builder()
                .employeeId(balance.getEmployeeId())
                .cpAnnuels(balance.getCpAnnuels())
                .rttAnnuels(balance.getRttAnnuels())
                .cpRestants(balance.getCpRestants())
                .rttRestants(balance.getRttRestants())
                .build();
    }
}

