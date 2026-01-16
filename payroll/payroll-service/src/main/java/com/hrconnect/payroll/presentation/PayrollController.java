package com.hrconnect.payroll.presentation;

import com.hrconnect.payroll.contract.EmployeeSalaryInfoDTO;
import com.hrconnect.payroll.contract.PayslipDTO;
import com.hrconnect.payroll.application.service.PayrollService;
import com.hrconnect.payroll.domain.model.PayslipHistory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller REST pour la gestion de la paie.
 *
 * Expose les endpoints pour :
 * - Générer/consulter les fiches de paie
 * - Consulter les informations salariales
 * - Historique des paies
 */
@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payroll", description = "API de gestion de la paie")
public class PayrollController {

    private final PayrollService payrollService;

    /**
     * Génère les données pour une fiche de paie.
     *
     * Agrège les informations de :
     * - Employee (salaire de base, infos personnelles)
     * - Interview (augmentations accordées)
     * - Leave (congés sans solde)
     */
    @GetMapping("/{employeeId}/payslip")
    @Operation(
            summary = "Générer une fiche de paie",
            description = "Retourne toutes les informations nécessaires pour éditer une fiche de paie. " +
                    "Agrège les données de Employee, Interview et Leave.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fiche de paie générée"),
                    @ApiResponse(responseCode = "404", description = "Employé non trouvé")
            }
    )
    public ResponseEntity<PayslipDTO> generatePayslip(
            @Parameter(description = "ID de l'employé") @PathVariable String employeeId,
            @Parameter(description = "Période au format YYYY-MM (défaut: mois courant)")
            @RequestParam(required = false) String month
    ) {
        // Si pas de mois spécifié, prendre le mois courant
        String period = month != null ? month : YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        log.info("Génération fiche de paie pour {} période {}", employeeId, period);

        try {
            PayslipDTO payslip = payrollService.generatePayslip(employeeId, period);
            return ResponseEntity.ok(payslip);
        } catch (IllegalArgumentException e) {
            log.warn("Employé non trouvé: {}", employeeId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Sauvegarde une fiche de paie dans l'historique
     */
    @PostMapping("/{employeeId}/payslip")
    @Operation(
            summary = "Sauvegarder une fiche de paie",
            description = "Génère et sauvegarde une fiche de paie dans l'historique"
    )
    public ResponseEntity<PayslipHistory> savePayslip(
            @PathVariable String employeeId,
            @RequestParam(required = false) String month
    ) {
        String period = month != null ? month : YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        log.info("Sauvegarde fiche de paie pour {} période {}", employeeId, period);
        PayslipHistory history = payrollService.savePayslipHistory(employeeId, period);
        return ResponseEntity.ok(history);
    }

    /**
     * Récupère les informations salariales d'un employé
     */
    @GetMapping("/{employeeId}/salary")
    @Operation(
            summary = "Informations salariales",
            description = "Retourne le salaire actuel avec le détail des augmentations"
    )
    public ResponseEntity<EmployeeSalaryInfoDTO> getEmployeeSalaryInfo(
            @PathVariable String employeeId
    ) {
        try {
            EmployeeSalaryInfoDTO salaryInfo = payrollService.getEmployeeSalaryInfo(employeeId);
            return ResponseEntity.ok(salaryInfo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Liste tous les employés avec leurs informations salariales
     */
    @GetMapping("/employees")
    @Operation(
            summary = "Liste des employés avec salaires",
            description = "Retourne tous les employés avec leurs informations salariales"
    )
    public ResponseEntity<List<EmployeeSalaryInfoDTO>> getAllEmployeesSalaryInfo() {
        List<EmployeeSalaryInfoDTO> employees = payrollService.getAllEmployeesSalaryInfo();
        return ResponseEntity.ok(employees);
    }

    /**
     * Historique des fiches de paie d'un employé
     */
    @GetMapping("/{employeeId}/history")
    @Operation(
            summary = "Historique des fiches de paie",
            description = "Retourne l'historique des fiches de paie générées pour un employé"
    )
    public ResponseEntity<List<PayslipHistory>> getPayslipHistory(
            @PathVariable String employeeId
    ) {
        List<PayslipHistory> history = payrollService.getPayslipHistory(employeeId);
        return ResponseEntity.ok(history);
    }
}
