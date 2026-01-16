package com.hrconnect.interview.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateInterviewRequest {

    private Double augmentationAccordee;

    private String feedback;
}
