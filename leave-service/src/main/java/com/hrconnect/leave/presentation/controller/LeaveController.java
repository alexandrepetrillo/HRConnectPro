package com.hrconnect.leave.presentation.controller;

import com.hrconnect.leave.domain.model.Leave;
import com.hrconnect.leave.application.service.LeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour les congés
 */
@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Leave", description = "API de gestion des congés")
public class LeaveController {

    private final LeaveService leaveService;

    @GetMapping
    @Operation(summary = "Récupérer tous les congés")
    public ResponseEntity<List<Leave>> getAllLeaves() {
        log.info("GET /api/leaves - Fetching all leaves");
        List<Leave> leaves = leaveService.getAllLeaves();
        return ResponseEntity.ok(leaves);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un congé par son ID")
    public ResponseEntity<Leave> getLeaveById(@PathVariable Long id) {
        log.info("GET /api/leaves/{} - Fetching leave", id);
        Leave leave = leaveService.getLeaveById(id);
        return ResponseEntity.ok(leave);
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Récupérer tous les congés d'un employé")
    public ResponseEntity<List<Leave>> getLeavesByEmployeeId(@PathVariable String employeeId) {
        log.info("GET /api/leaves/employee/{} - Fetching leaves for employee", employeeId);
        List<Leave> leaves = leaveService.getLeavesByEmployeeId(employeeId);
        return ResponseEntity.ok(leaves);
    }

    @PostMapping
    @Operation(summary = "Créer un nouveau congé")
    public ResponseEntity<Leave> createLeave(@RequestBody Leave leave) {
        log.info("POST /api/leaves - Creating new leave");
        Leave createdLeave = leaveService.createLeave(leave);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdLeave);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un congé")
    public ResponseEntity<Leave> updateLeave(@PathVariable Long id, @RequestBody Leave leave) {
        log.info("PUT /api/leaves/{} - Updating leave", id);
        Leave updatedLeave = leaveService.updateLeave(id, leave);
        return ResponseEntity.ok(updatedLeave);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un congé")
    public ResponseEntity<Void> deleteLeave(@PathVariable Long id) {
        log.info("DELETE /api/leaves/{} - Deleting leave", id);
        leaveService.deleteLeave(id);
        return ResponseEntity.noContent().build();
    }
}

