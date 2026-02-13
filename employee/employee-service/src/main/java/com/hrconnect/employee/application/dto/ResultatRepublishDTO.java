package com.hrconnect.employee.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ResultatRepublishDTO {
  private String message ;
  private int employeeCount;
  private String triggeredBy;
  private LocalDateTime triggeredAt;
}
