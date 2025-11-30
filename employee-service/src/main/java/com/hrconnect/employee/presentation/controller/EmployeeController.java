package com.hrconnect.employee.presentation.controller;

import com.hrconnect.employee.application.dto.EmployeeDTO;
import com.hrconnect.employee.application.mapper.EmployeeMapper;
import com.hrconnect.employee.application.service.EmployeeService;
import com.hrconnect.employee.domain.model.Employee;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Contrôleur REST pour la gestion des employés
 */
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Tag(name = "Employees", description = "API de gestion des employés")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeMapper employeeMapper;

    @GetMapping
    @Operation(summary = "Récupérer tous les employés")
    public ResponseEntity<List<EmployeeDTO>> getAllEmployees() {
        List<EmployeeDTO> employees = employeeService.getAllEmployees().stream()
            .map(employeeMapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/{reference}")
    @Operation(summary = "Récupérer un employé par sa référence")
    public ResponseEntity<EmployeeDTO> getEmployeeById(@PathVariable String reference) {
        return employeeService.getEmployeeById(reference)
            .map(employeeMapper::toDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/departement/{departement}")
    @Operation(summary = "Récupérer les employés par département")
    public ResponseEntity<List<EmployeeDTO>> getEmployeesByDepartement(@PathVariable String departement) {
        List<EmployeeDTO> employees = employeeService.getEmployeesByDepartement(departement).stream()
            .map(employeeMapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(employees);
    }

    @PostMapping
    @Operation(summary = "Créer un nouvel employé")
    public ResponseEntity<EmployeeDTO> createEmployee(@Valid @RequestBody EmployeeDTO employeeDTO) {
        Employee employee = employeeMapper.toEntity(employeeDTO);
        Employee created = employeeService.createEmployee(employee);
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeMapper.toDTO(created));
    }

    @PutMapping("/{reference}")
    @Operation(summary = "Mettre à jour un employé")
    public ResponseEntity<EmployeeDTO> updateEmployee(
            @PathVariable String reference,
            @Valid @RequestBody EmployeeDTO employeeDTO) {
        Employee employee = employeeMapper.toEntity(employeeDTO);
        Employee updated = employeeService.updateEmployee(reference, employee);
        return ResponseEntity.ok(employeeMapper.toDTO(updated));
    }

    @DeleteMapping("/{reference}")
    @Operation(summary = "Supprimer un employé")
    public ResponseEntity<Void> deleteEmployee(@PathVariable String reference) {
        employeeService.deleteEmployee(reference);
        return ResponseEntity.noContent().build();
    }
}

