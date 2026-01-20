package com.hrconnect.employee;

import com.hrconnect.employee.application.dto.EmployeeDTO;
import com.hrconnect.employee.domain.model.Employee;
import com.hrconnect.employee.domain.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();
    }

    @Test
    void shouldCreateEmployee() {
        // Given
        EmployeeDTO employeeDTO = EmployeeDTO.builder()
            .reference("E001")
            .nom("Dupont")
            .prenom("Alice")
            .numeroSecuriteSociale("185057512345678")
            .dateNaissance(LocalDate.of(1985, 5, 15))
            .email("alice.dupont@company.com")
            .telephone("+33123456789")
            .role("Manager")
            .departement("IT")
            .contrat(EmployeeDTO.ContratDTO.builder()
                .type("CDI")
                .debut(LocalDate.of(2022, 3, 1))
                .build())
            .salaireAnnuelBase(48000.0)
            .build();

        HttpEntity<EmployeeDTO> request = new HttpEntity<>(employeeDTO, createHrAuthHeaders());

        // When
        ResponseEntity<EmployeeDTO> response = restTemplate.exchange(
            url("/api/employees"),
            HttpMethod.POST,
            request,
            EmployeeDTO.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getReference()).isEqualTo("E001");
        assertThat(response.getBody().getNom()).isEqualTo("Dupont");
        assertThat(response.getBody().getPrenom()).isEqualTo("Alice");

        // Vérifier en base
        Employee saved = employeeRepository.findByReference("E001").orElseThrow();
        assertThat(saved.getNom()).isEqualTo("Dupont");
        assertThat(saved.getPrenom()).isEqualTo("Alice");
    }

    @Test
    void shouldGetEmployeeById() {
        // Given
        Employee employee = Employee.builder()
            .reference("E002")
            .nom("Martin")
            .prenom("Bob")
            .numeroSecuriteSociale("190017512345789")
            .dateNaissance(LocalDate.of(1990, 1, 15))
            .email("bob.martin@company.com")
            .role("Developer")
            .departement("IT")
            .contrat(Employee.Contrat.builder()
                .type("CDI")
                .debut(LocalDate.of(2023, 1, 1))
                .build())
            .salaireAnnuelBase(42000.0)
            .build();
        employeeRepository.save(employee);

        HttpEntity<Void> request = new HttpEntity<>(createHrAuthHeaders());

        // When
        ResponseEntity<EmployeeDTO> response = restTemplate.exchange(
            url("/api/employees/E002"),
            HttpMethod.GET,
            request,
            EmployeeDTO.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getReference()).isEqualTo("E002");
        assertThat(response.getBody().getNom()).isEqualTo("Martin");
        assertThat(response.getBody().getPrenom()).isEqualTo("Bob");
    }

    @Test
    void shouldReturnNotFoundForNonExistentEmployee() {
        HttpEntity<Void> request = new HttpEntity<>(createHrAuthHeaders());

        // When
        ResponseEntity<EmployeeDTO> response = restTemplate.exchange(
            url("/api/employees/E999"),
            HttpMethod.GET,
            request,
            EmployeeDTO.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnUnauthorizedWithoutToken() {
        // When - Requête sans token JWT
        ResponseEntity<String> response = restTemplate.getForEntity(
            url("/api/employees/E001"),
            String.class
        );

        // Then - Doit retourner 401 Unauthorized
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturnForbiddenWithWrongRole() {
        // Given - Token avec un rôle non autorisé (USER au lieu de HR)
        HttpEntity<Void> request = new HttpEntity<>(createAuthHeadersWithRole("ROLE_USER"));

        // When
        ResponseEntity<String> response = restTemplate.exchange(
            url("/api/employees/E001"),
            HttpMethod.GET,
            request,
            String.class
        );

        // Then - Doit retourner 403 Forbidden
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
