package com.hrconnect.payroll.application.service;

import com.hrconnect.payroll.contract.EmployeeSalaryInfoDTO;
import com.hrconnect.payroll.contract.PayslipDTO;
import com.hrconnect.payroll.domain.model.*;
import com.hrconnect.payroll.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service métier pour la gestion de la paie.
 *
 * Ce service agrège les données de plusieurs sources (snapshots) :
 * - EmployeeSnapshot : salaire de base, informations personnelles
 * - InterviewSnapshot : augmentations accordées
 * - LeaveSnapshot : congés sans solde (déductions)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PayrollService {

    private static final int JOURS_TRAVAILLES_PAR_MOIS = 22; // Moyenne
    private static final int MOIS_PAR_AN = 12;

    private final EmployeeSnapshotRepository employeeSnapshotRepository;
    private final InterviewSnapshotRepository interviewSnapshotRepository;
    private final LeaveSnapshotRepository leaveSnapshotRepository;
    private final PayslipHistoryRepository payslipHistoryRepository;

    /**
     * Génère les données pour éditer une fiche de paie.
     *
     * @param employeeId ID de l'employé
     * @param month Période de paie au format YYYY-MM
     * @return DTO contenant toutes les informations de paie
     */
    @Transactional(readOnly = true)
    public PayslipDTO generatePayslip(String employeeId, String month) {
        log.info("Génération de la fiche de paie pour employé {} période {}", employeeId, month);

        // 1. Récupérer les informations de l'employé
        EmployeeSnapshot employee = employeeSnapshotRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employé non trouvé: " + employeeId));

        // 2. Parser la période
        YearMonth yearMonth = YearMonth.parse(month);
        LocalDate periodStart = yearMonth.atDay(1);
        LocalDate periodEnd = yearMonth.atEndOfMonth();

        // 3. Récupérer les augmentations validées
        List<InterviewSnapshot> augmentations = interviewSnapshotRepository
                .findValidatedAugmentationsUpToDate(employeeId, periodEnd);

        // 4. Calculer le total des augmentations
        Double totalAugmentations = augmentations.stream()
                .mapToDouble(InterviewSnapshot::getAugmentationAccordee)
                .sum();

        // 5. Calculer le salaire actuel
        Double salaireBase = employee.getSalaireAnnuelBase();
        Double salaireActuel = salaireBase + totalAugmentations;

        // 6. Récupérer les congés sans solde sur la période
        List<LeaveSnapshot> congesSansSolde = leaveSnapshotRepository
                .findCongesSansSoldeForPeriod(employeeId, periodStart, periodEnd);

        // 7. Calculer les jours de congés sans solde dans la période
        int totalJoursCongesSansSolde = calculateJoursCongesSansSoldeDansPeriode(
                congesSansSolde, periodStart, periodEnd);

        // 8. Calculer la déduction pour congés sans solde
        Double salaireMensuelBrut = salaireActuel / MOIS_PAR_AN;
        Double tauxJournalier = salaireMensuelBrut / JOURS_TRAVAILLES_PAR_MOIS;
        Double deductionCongesSansSolde = totalJoursCongesSansSolde * tauxJournalier;

        // 9. Calculer le salaire brut mensuel final
        Double salaireBrutMensuel = salaireMensuelBrut - deductionCongesSansSolde;

        // 10. Construire le DTO de réponse
        return PayslipDTO.builder()
                .employee(buildEmployeeInfo(employee))
                .period(month)
                .salaireBase(salaireBase)
                .augmentations(buildAugmentationInfoList(augmentations))
                .totalAugmentations(totalAugmentations)
                .salaireActuel(salaireActuel)
                .congesSansSolde(buildCongesSansSoldeInfoList(congesSansSolde, periodStart, periodEnd))
                .totalJoursCongesSansSolde(totalJoursCongesSansSolde)
                .deductionCongesSansSolde(Math.round(deductionCongesSansSolde * 100.0) / 100.0)
                .salaireBrutMensuel(Math.round(salaireBrutMensuel * 100.0) / 100.0)
                .build();
    }

    /**
     * Sauvegarde une fiche de paie dans l'historique
     */
    @Transactional
    public PayslipHistory savePayslipHistory(String employeeId, String month) {
        PayslipDTO payslip = generatePayslip(employeeId, month);

        PayslipHistory history = PayslipHistory.builder()
                .employeeId(employeeId)
                .periodMonth(month)
                .salaireBase(payslip.getSalaireBase())
                .totalAugmentations(payslip.getTotalAugmentations())
                .salaireActuel(payslip.getSalaireActuel())
                .joursCongesSansSolde(payslip.getTotalJoursCongesSansSolde())
                .deductionCongesSansSolde(payslip.getDeductionCongesSansSolde())
                .salaireBrutMensuel(payslip.getSalaireBrutMensuel())
                .build();

        return payslipHistoryRepository.save(history);
    }

    /**
     * Récupère les informations salariales d'un employé
     */
    @Transactional(readOnly = true)
    public EmployeeSalaryInfoDTO getEmployeeSalaryInfo(String employeeId) {
        EmployeeSnapshot employee = employeeSnapshotRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employé non trouvé: " + employeeId));

        List<InterviewSnapshot> augmentations = interviewSnapshotRepository
                .findValidatedAugmentations(employeeId);

        Double totalAugmentations = augmentations.stream()
                .mapToDouble(InterviewSnapshot::getAugmentationAccordee)
                .sum();

        Double salaireActuel = employee.getSalaireAnnuelBase() + totalAugmentations;

        return EmployeeSalaryInfoDTO.builder()
                .employeeId(employeeId)
                .reference(employee.getReference())
                .nom(employee.getNom())
                .email(employee.getEmail())
                .departement(employee.getDepartement())
                .salaireBase(employee.getSalaireAnnuelBase())
                .totalAugmentations(totalAugmentations)
                .salaireActuel(salaireActuel)
                .salaireMensuelBrut(Math.round(salaireActuel / MOIS_PAR_AN * 100.0) / 100.0)
                .nombreAugmentations(augmentations.size())
                .build();
    }

    /**
     * Liste tous les employés avec leurs informations salariales
     */
    @Transactional(readOnly = true)
    public List<EmployeeSalaryInfoDTO> getAllEmployeesSalaryInfo() {
        return employeeSnapshotRepository.findAll().stream()
                .map(emp -> getEmployeeSalaryInfo(emp.getEmployeeId()))
                .collect(Collectors.toList());
    }

    /**
     * Récupère l'historique des fiches de paie d'un employé
     */
    @Transactional(readOnly = true)
    public List<PayslipHistory> getPayslipHistory(String employeeId) {
        return payslipHistoryRepository.findByEmployeeIdOrderByPeriodMonthDesc(employeeId);
    }

    // === Méthodes privées ===

    private PayslipDTO.EmployeeInfo buildEmployeeInfo(EmployeeSnapshot employee) {
        return PayslipDTO.EmployeeInfo.builder()
                .reference(employee.getReference())
                .nom(employee.getNom())
                .email(employee.getEmail())
                .telephone(employee.getTelephone())
                .departement(employee.getDepartement())
                .role(employee.getRole())
                .contratType(employee.getContratType())
                .build();
    }

    private List<PayslipDTO.AugmentationInfo> buildAugmentationInfoList(List<InterviewSnapshot> augmentations) {
        return augmentations.stream()
                .map(aug -> PayslipDTO.AugmentationInfo.builder()
                        .date(aug.getDateEntretien().format(DateTimeFormatter.ISO_DATE))
                        .montant(aug.getAugmentationAccordee())
                        .type(aug.getType())
                        .reference(aug.getReference())
                        .build())
                .collect(Collectors.toList());
    }

    private List<PayslipDTO.CongesSansSoldeInfo> buildCongesSansSoldeInfoList(
            List<LeaveSnapshot> conges, LocalDate periodStart, LocalDate periodEnd) {
        return conges.stream()
                .map(conge -> {
                    // Calculer les jours effectifs dans la période
                    LocalDate effectiveStart = conge.getDateDebut().isBefore(periodStart)
                            ? periodStart : conge.getDateDebut();
                    LocalDate effectiveEnd = conge.getDateFin().isAfter(periodEnd)
                            ? periodEnd : conge.getDateFin();
                    int jours = (int) ChronoUnit.DAYS.between(effectiveStart, effectiveEnd) + 1;

                    return PayslipDTO.CongesSansSoldeInfo.builder()
                            .dateDebut(effectiveStart.format(DateTimeFormatter.ISO_DATE))
                            .dateFin(effectiveEnd.format(DateTimeFormatter.ISO_DATE))
                            .jours(jours)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private int calculateJoursCongesSansSoldeDansPeriode(
            List<LeaveSnapshot> conges, LocalDate periodStart, LocalDate periodEnd) {
        return conges.stream()
                .mapToInt(conge -> {
                    LocalDate effectiveStart = conge.getDateDebut().isBefore(periodStart)
                            ? periodStart : conge.getDateDebut();
                    LocalDate effectiveEnd = conge.getDateFin().isAfter(periodEnd)
                            ? periodEnd : conge.getDateFin();
                    return (int) ChronoUnit.DAYS.between(effectiveStart, effectiveEnd) + 1;
                })
                .sum();
    }
}
