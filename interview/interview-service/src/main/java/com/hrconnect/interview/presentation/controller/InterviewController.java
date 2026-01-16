package com.hrconnect.interview.presentation.controller;

import com.hrconnect.interview.application.service.InterviewService;
import com.hrconnect.interview.domain.model.Interview;
import com.hrconnect.interview.domain.model.InterviewStatus;
import com.hrconnect.interview.presentation.dto.InterviewRequest;
import com.hrconnect.interview.presentation.dto.ValidateInterviewRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
@Tag(name = "Interviews", description = "API de gestion des entretiens")
public class InterviewController {

    private final InterviewService interviewService;

    @GetMapping
    @Operation(summary = "Liste tous les entretiens")
    public ResponseEntity<List<Interview>> findAll() {
        return ResponseEntity.ok(interviewService.findAll());
    }

    @GetMapping("/{reference}")
    @Operation(summary = "Récupère un entretien par sa référence")
    public ResponseEntity<Interview> findByReference(@PathVariable String reference) {
        return ResponseEntity.ok(interviewService.findByReference(reference));
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Liste les entretiens d'un employé")
    public ResponseEntity<List<Interview>> findByEmployeeId(@PathVariable String employeeId) {
        return ResponseEntity.ok(interviewService.findByEmployeeId(employeeId));
    }

    @PostMapping
    @Operation(summary = "Crée un nouvel entretien")
    public ResponseEntity<Interview> create(@Valid @RequestBody InterviewRequest request) {
        Interview created = interviewService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{reference}/status")
    @Operation(summary = "Met à jour le statut d'un entretien")
    public ResponseEntity<Interview> updateStatus(
            @PathVariable String reference,
            @RequestParam InterviewStatus status) {
        return ResponseEntity.ok(interviewService.updateStatus(reference, status));
    }

    @PostMapping("/{reference}/validate")
    @Operation(summary = "Valide un entretien avec augmentation")
    public ResponseEntity<Interview> validate(
            @PathVariable String reference,
            @RequestBody ValidateInterviewRequest request) {
        return ResponseEntity.ok(
            interviewService.validate(reference, request.getAugmentationAccordee(), request.getFeedback())
        );
    }
}
