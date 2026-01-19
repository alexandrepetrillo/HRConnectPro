package com.hrconnect.employee.presentation.controller;

import com.hrconnect.employee.application.dto.EmployeeDTO;
import com.hrconnect.employee.application.mapper.EmployeeMapper;
import com.hrconnect.employee.application.service.EmployeeService;
import com.hrconnect.employee.domain.model.Employee;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Contrôleur REST pour la gestion des employés
 *
 * Permissions :
 * - ADMIN : Tout (lecture, création, modification, suppression)
 * - MANAGER : Lecture, création, modification (PAS de suppression)
 * - USER (ou autre) : Uniquement accès à ses propres informations via /me
 */
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Tag(name = "Employees", description = "API de gestion des employés")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeMapper employeeMapper;

    /**
     * Récupérer ses propres informations (accessible à tous les utilisateurs authentifiés)
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Récupérer mes propres informations")
    public ResponseEntity<EmployeeDTO> getMyInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        // Recherche l'employé dont la référence correspond au username
        return employeeService.getEmployeeById(username)
            .map(employeeMapper::toDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @RolesAllowed({"ADMIN", "MANAGER"})
    @Operation(summary = "Récupérer tous les employés")
    public ResponseEntity<List<EmployeeDTO>> getAllEmployees() {
        List<EmployeeDTO> employees = employeeService.getAllEmployees().stream()
            .map(employeeMapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/{reference}")
    @RolesAllowed({"ADMIN", "MANAGER"})
    @Operation(summary = "Récupérer un employé par sa référence")
    public ResponseEntity<EmployeeDTO> getEmployeeById(@PathVariable String reference) {
        return employeeService.getEmployeeById(reference)
            .map(employeeMapper::toDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/departement/{departement}")
    @RolesAllowed({"ADMIN", "MANAGER"})
    @Operation(summary = "Récupérer les employés par département")
    public ResponseEntity<List<EmployeeDTO>> getEmployeesByDepartement(@PathVariable String departement) {
        List<EmployeeDTO> employees = employeeService.getEmployeesByDepartement(departement).stream()
            .map(employeeMapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/manager/{managerId}")
    @RolesAllowed({"ADMIN", "MANAGER"})
    @Operation(summary = "Récupérer les employés par manager")
    public ResponseEntity<List<EmployeeDTO>> getEmployeesByManager(@PathVariable String managerId) {
        List<EmployeeDTO> employees = employeeService.getEmployeesByManager(managerId).stream()
            .map(employeeMapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(employees);
    }

    @PostMapping
    @RolesAllowed({"ADMIN", "MANAGER"})
    @Operation(summary = "Créer un nouvel employé")
    public ResponseEntity<EmployeeDTO> createEmployee(@Valid @RequestBody EmployeeDTO employeeDTO) {
        Employee employee = employeeMapper.toEntity(employeeDTO);
        Employee created = employeeService.createEmployee(employee);
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeMapper.toDTO(created));
    }

    @PutMapping("/{reference}")
    @RolesAllowed({"ADMIN", "MANAGER"})
    @Operation(summary = "Mettre à jour un employé")
    public ResponseEntity<EmployeeDTO> updateEmployee(
            @PathVariable String reference,
            @Valid @RequestBody EmployeeDTO employeeDTO) {
        Employee employee = employeeMapper.toEntity(employeeDTO);
        Employee updated = employeeService.updateEmployee(reference, employee);
        return ResponseEntity.ok(employeeMapper.toDTO(updated));
    }

    @DeleteMapping("/{reference}")
    @RolesAllowed("ADMIN")  // Seul ADMIN peut supprimer
    @Operation(summary = "Supprimer un employé (ADMIN uniquement)")
    public ResponseEntity<Void> deleteEmployee(@PathVariable String reference) {
        employeeService.deleteEmployee(reference);
        return ResponseEntity.noContent().build();
    }
}

