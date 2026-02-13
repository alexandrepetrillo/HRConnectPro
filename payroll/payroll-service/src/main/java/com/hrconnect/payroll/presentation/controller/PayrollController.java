package com.hrconnect.payroll.presentation.controller;

import com.hrconnect.payroll.application.service.PayrollService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payroll", description = "API de gestion des fiches de paie")

public class PayrollController {

  private final PayrollService payrollService;

  @PostMapping("{employeeId}/calculate")
  public void calculate(
    @PathVariable String employeeId,
    @RequestParam int month,
    @RequestParam int year,
    HttpServletRequest httpServletRequest) {

    String authHeader = httpServletRequest.getHeader("Authorization");
    log.info("Calculating payroll for employee {} for {}/{}", employeeId, month, year);
    payrollService.calculer(employeeId, month, year, authHeader);
  }
}
