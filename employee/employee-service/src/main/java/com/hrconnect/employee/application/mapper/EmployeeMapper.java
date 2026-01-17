package com.hrconnect.employee.application.mapper;

import com.hrconnect.employee.application.dto.EmployeeDTO;
import com.hrconnect.employee.domain.model.Employee;
import org.springframework.stereotype.Component;

/**
 * Mapper entre Employee et EmployeeDTO
 */
@Component
public class EmployeeMapper {

    public Employee toEntity(EmployeeDTO dto) {
        if (dto == null) {
            return null;
        }

        return Employee.builder()
            .reference(dto.getReference())
            .nom(dto.getNom())
            .numeroSecuriteSociale(dto.getNumeroSecuriteSociale())
            .dateNaissance(dto.getDateNaissance())
            .email(dto.getEmail())
            .telephone(dto.getTelephone())
            .role(dto.getRole())
            .departement(dto.getDepartement())
            .managerId(dto.getManagerId())
            .contrat(dto.getContrat() != null ? Employee.Contrat.builder()
                .type(dto.getContrat().getType())
                .debut(dto.getContrat().getDebut())
                .fin(dto.getContrat().getFin())
                .build() : null)
            .salaireAnnuelBase(dto.getSalaireAnnuelBase())
            .build();
    }

    public EmployeeDTO toDTO(Employee entity) {
        if (entity == null) {
            return null;
        }

        return EmployeeDTO.builder()
            .reference(entity.getReference())
            .nom(entity.getNom())
            .numeroSecuriteSociale(entity.getNumeroSecuriteSociale())
            .dateNaissance(entity.getDateNaissance())
            .email(entity.getEmail())
            .telephone(entity.getTelephone())
            .role(entity.getRole())
            .departement(entity.getDepartement())
            .managerId(entity.getManagerId())
            .contrat(entity.getContrat() != null ? EmployeeDTO.ContratDTO.builder()
                .type(entity.getContrat().getType())
                .debut(entity.getContrat().getDebut())
                .fin(entity.getContrat().getFin())
                .build() : null)
            .salaireAnnuelBase(entity.getSalaireAnnuelBase())
            .build();
    }
}

