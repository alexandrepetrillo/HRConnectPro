package com.hrconnect.leave.application.service;

import com.hrconnect.leave.domain.exception.EmployeeNotFoundException;
import com.hrconnect.leave.domain.exception.InvalidLeaveDatesException;
import com.hrconnect.leave.domain.exception.InvalidLeaveStatusException;
import com.hrconnect.leave.domain.exception.LeaveNotFoundException;
import com.hrconnect.leave.domain.model.Leave;
import com.hrconnect.leave.domain.model.LeaveStatus;
import com.hrconnect.leave.domain.repository.EmployeeSnapshotRepository;
import com.hrconnect.leave.domain.repository.LeaveRepository;
import com.hrconnect.leave.infrastructure.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service métier pour la gestion des congés
 * Utilise le pattern Outbox pour garantir la cohérence transactionnelle
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveService {

    private final LeaveRepository leaveRepository;
    private final EmployeeSnapshotRepository employeeSnapshotRepository;
    private final OutboxService outboxService;

    /**
     * Crée un nouveau congé
     */
    @Transactional
    public Leave createLeave(Leave leave) {
        log.info("Creating leave for employee: {}", leave.getEmployeeId());

        // 1. Vérifier que l'employé existe dans le snapshot local
        if (!employeeSnapshotRepository.existsByEmployeeId(leave.getEmployeeId())) {
            throw new EmployeeNotFoundException(leave.getEmployeeId());
        }

        // 2. Valider les dates
        validateDates(leave);

        // 3. Calculer le nombre de jours posés
        long joursPoses = ChronoUnit.DAYS.between(leave.getDateDebut(), leave.getDateFin()) + 1;
        leave.setJoursPoses((int) joursPoses);

        // 4. Calculer les jours travaillés et posés dans le mois (basé sur le mois de début)
        int joursTravaillesMois = calculateWorkingDaysInMonth(leave.getDateDebut());
        leave.setJoursTravaillesMois(joursTravaillesMois);
        leave.setJoursPosesMois((int) joursPoses); // Simplifié : on considère tous les jours posés dans le même mois

        // 5. Définir le statut par défaut si non renseigné
        if (leave.getStatut() == null) {
            leave.setStatut(LeaveStatus.EN_ATTENTE);
        }

        // 6. Sauvegarder
        Leave savedLeave = leaveRepository.save(leave);
        log.info("Leave created with id: {}", savedLeave.getId());

        // 7. Enregistrer dans l'Outbox (dans la même transaction)
        outboxService.saveLeaveState(savedLeave);

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
                .orElseThrow(() -> new LeaveNotFoundException(String.valueOf(id)));
    }

    /**
     * Met à jour un congé
     */
    @Transactional
    public Leave updateLeave(Long id, Leave leaveDetails) {
        log.info("Updating leave with id: {}", id);

        Leave leave = getLeaveById(id);

        // Mettre à jour les champs modifiables
        if (leaveDetails.getType() != null) {
            leave.setType(leaveDetails.getType());
        }
        if (leaveDetails.getDateDebut() != null) {
            leave.setDateDebut(leaveDetails.getDateDebut());
        }
        if (leaveDetails.getDateFin() != null) {
            leave.setDateFin(leaveDetails.getDateFin());
        }
        if (leaveDetails.getStatut() != null) {
            leave.setStatut(leaveDetails.getStatut());
        }
        if (leaveDetails.getCommentaire() != null) {
            leave.setCommentaire(leaveDetails.getCommentaire());
        }
        if (leaveDetails.getJoursTravaillesMois() != null) {
            leave.setJoursTravaillesMois(leaveDetails.getJoursTravaillesMois());
        }
        if (leaveDetails.getJoursPosesMois() != null) {
            leave.setJoursPosesMois(leaveDetails.getJoursPosesMois());
        }

        // Recalculer les jours posés si les dates ont changé
        validateDates(leave);
        long joursPoses = ChronoUnit.DAYS.between(leave.getDateDebut(), leave.getDateFin()) + 1;
        leave.setJoursPoses((int) joursPoses);

        Leave updatedLeave = leaveRepository.save(leave);

        // Enregistrer dans l'Outbox (dans la même transaction)
        outboxService.saveLeaveState(updatedLeave);

        return updatedLeave;
    }

    /**
     * Valide un congé (approuve)
     */
    @Transactional
    public Leave approveLeave(Long id) {
        log.info("Approving leave with id: {}", id);
        Leave leave = getLeaveById(id);

        if (leave.getStatut() != LeaveStatus.EN_ATTENTE) {
            throw new InvalidLeaveStatusException("Can only approve leaves that are in EN_ATTENTE status");
        }

        leave.setStatut(LeaveStatus.VALIDE);
        Leave approvedLeave = leaveRepository.save(leave);

        // Enregistrer dans l'Outbox (dans la même transaction)
        outboxService.saveLeaveState(approvedLeave);

        return approvedLeave;
    }

    /**
     * Refuse un congé
     */
    @Transactional
    public Leave rejectLeave(Long id, String reason) {
        log.info("Rejecting leave with id: {}", id);
        Leave leave = getLeaveById(id);

        if (leave.getStatut() != LeaveStatus.EN_ATTENTE) {
            throw new InvalidLeaveStatusException("Can only reject leaves that are in EN_ATTENTE status");
        }

        leave.setStatut(LeaveStatus.REFUSE);
        leave.setCommentaire(reason);
        Leave rejectedLeave = leaveRepository.save(leave);

        // Enregistrer dans l'Outbox (dans la même transaction)
        outboxService.saveLeaveState(rejectedLeave);

        return rejectedLeave;
    }

    /**
     * Annule un congé
     */
    @Transactional
    public Leave cancelLeave(Long id) {
        log.info("Cancelling leave with id: {}", id);
        Leave leave = getLeaveById(id);

        if (leave.getStatut() == LeaveStatus.ANNULE) {
            throw new InvalidLeaveStatusException("Leave is already cancelled");
        }

        leave.setStatut(LeaveStatus.ANNULE);
        Leave cancelledLeave = leaveRepository.save(leave);

        // Enregistrer dans l'Outbox (dans la même transaction)
        outboxService.saveLeaveState(cancelledLeave);

        return cancelledLeave;
    }

    /**
     * Supprime un congé
     */
    @Transactional
    public void deleteLeave(Long id) {
        log.info("Deleting leave with id: {}", id);
        if (!leaveRepository.existsById(id)) {
            throw new LeaveNotFoundException(String.valueOf(id));
        }
        leaveRepository.deleteById(id);
    }

    private void validateDates(Leave leave) {
        if (leave.getDateDebut() == null || leave.getDateFin() == null) {
            throw new InvalidLeaveDatesException("Date de début et date de fin sont obligatoires");
        }
        if (leave.getDateDebut().isAfter(leave.getDateFin())) {
            throw new InvalidLeaveDatesException("Date de début doit être avant ou égale à la date de fin");
        }
    }

    /**
     * Calcule le nombre de jours ouvrés dans le mois de la date donnée.
     * Convention : 22 jours ouvrés par mois (moyenne standard).
     */
    private int calculateWorkingDaysInMonth(LocalDate date) {
        // Convention standard : 22 jours ouvrés par mois
        return 22;
    }
}

