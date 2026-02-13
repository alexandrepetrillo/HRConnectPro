package com.hrconnect.employee;

import com.hrconnect.employee.application.dto.EmployeeDTO;
import com.hrconnect.employee.domain.model.Employee;
import com.hrconnect.employee.domain.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;

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
      .email("alice.dupont@company.com")
      .telephone("+33123456789")
      .dateNaissance(LocalDate.now())
      .numeroSecuriteSociale("123456789012012")
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

    // Vérifier en base
    Employee saved = employeeRepository.findByReference("E001").orElseThrow();
    assertThat(saved.getNom()).isEqualTo("Dupont");
  }

  @Test
  void shouldGetEmployeeById() {
    // Given
    Employee employee = Employee.builder()
      .reference("E002")
      .nom("Martin")
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

  @Test
  void findActifs() {
    LocalDate now = LocalDate.of(2022, 3, 1);
    LocalDate yesterday = now.minusDays(1);
    LocalDate beforeYesterday = now.minusDays(2);
    LocalDate tomorrow = now.plusDays(1);
    LocalDate future = now.plusDays(5);

    // Cas ACTIFS (doivent être retournés)
    Employee e1 = createEmployee(yesterday, null);      // debut < now, fin = null
    Employee e3 = createEmployee(now, null);            // debut = now, fin = null (cas limite début)
    Employee e3bis = createEmployee(yesterday, now);    // debut < now, fin = now (cas limite fin)
    Employee e4 = createEmployee(yesterday, future);    // debut < now, fin > now
    Employee e6 = createEmployee(now, now);             // debut = now, fin = now (contrat d'un jour)
    Employee e7 = createEmployee(now, future);          // debut = now, fin > now

    // Cas NON ACTIFS (ne doivent pas être retournés)
    Employee e2 = createEmployee(tomorrow, null);       // debut > now (contrat pas encore commencé)
    Employee e5 = createEmployee(beforeYesterday, yesterday); // fin < now (contrat terminé)
    Employee e8 = createEmployee(tomorrow, future);     // debut > now, fin > now (contrat futur)

    assertThat(employeeRepository.findActifs(now)).containsExactlyInAnyOrder(e1, e3, e3bis, e4, e6, e7);
  }


  @Test
  void findByContratDebutBeforeAndContratFinAfter() {
    LocalDate now = LocalDate.of(2022, 3, 1);
    LocalDate yesterday = now.minusDays(1);
    LocalDate beforeYesterday = now.minusDays(2);
    LocalDate tomorrow = now.plusDays(1);
    LocalDate future = now.plusDays(5);

    // Cas ACTIFS (doivent être retournés)
    Employee e1 = createEmployee(yesterday, null);      // debut < now, fin = null
    Employee e3 = createEmployee(now, null);            // debut = now, fin = null (cas limite début)
    Employee e3bis = createEmployee(yesterday, now);    // debut < now, fin = now (cas limite fin)
    Employee e4 = createEmployee(yesterday, future);    // debut < now, fin > now
    Employee e6 = createEmployee(now, now);             // debut = now, fin = now (contrat d'un jour)
    Employee e7 = createEmployee(now, future);          // debut = now, fin > now

    // Cas NON ACTIFS (ne doivent pas être retournés)
    Employee e2 = createEmployee(tomorrow, null);       // debut > now (contrat pas encore commencé)
    Employee e5 = createEmployee(beforeYesterday, yesterday); // fin < now (contrat terminé)
    Employee e8 = createEmployee(tomorrow, future);     // debut > now, fin > now (contrat futur)

    assertThat(employeeRepository.findByContratDebutBeforeAndContratFinAfter(now, now)).containsExactlyInAnyOrder(e1, e3, e3bis, e4, e6, e7);
  }

  private Employee createEmployee(LocalDate debut, LocalDate fin) {
    Employee employee = Employee.builder()
      .reference("E002" + System.nanoTime() % 1000)
      .nom("Martin")
      .email("bob.martin@company.com" + System.nanoTime() % 1000)
      .role("Developer")
      .departement("IT")
      .contrat(Employee.Contrat.builder()
        .type("CDI")
        .debut(debut)
        .fin(fin)
        .build())
      .salaireAnnuelBase(42000.0)
      .build();
    employeeRepository.save(employee);
    return employee;
  }
}