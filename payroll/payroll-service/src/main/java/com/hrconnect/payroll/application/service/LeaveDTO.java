package com.hrconnect.payroll.application.service;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LeaveDTO {

  private LeaveType type;

  private LocalDate dateDebut;

  private LocalDate dateFin;

  private LeaveStatus statut;

  private Integer joursPoses;
}
