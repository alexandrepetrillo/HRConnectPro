package com.hrconnect.leave.presentation.controller;

import com.hrconnect.leave.application.dto.InitializeLeaveBalanceRequest;
import com.hrconnect.leave.application.dto.LeaveBalanceResponse;
import com.hrconnect.leave.application.service.LeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour les compteurs de congés
 */
@RestController
@RequestMapping("/api/leave-balances")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Leave Balance", description = "API de gestion des compteurs de congés")
public class LeaveBalanceController {

    private final LeaveService leaveService;

    @GetMapping("/{employeeId}")
    @Operation(summary = "Récupérer les compteurs de congés d'un employé")
    public ResponseEntity<LeaveBalanceResponse> getLeaveBalance(@PathVariable String employeeId) {
        log.info("GET /api/leave-balances/{} - Fetching leave balance", employeeId);

        LeaveBalanceResponse response = leaveService.getLeaveBalance(employeeId);
        return ResponseEntity.ok(response);
    }
}
