package com.hrconnect.employee;

import com.hrconnect.employee.application.dto.EmployeeDTO;
import com.hrconnect.employee.domain.model.Employee;
import com.hrconnect.employee.domain.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EmployeeServiceIntegrationTest {

  // ==================== Conteneurs Testcontainers ====================

  protected static DockerComposeContainer<?> environment;

  static {
    // Charge docker-compose.test.yml qui contient la configuration complète pour les tests
    // avec ports dynamiques pour éviter les conflits avec l'environnement de dev local
    // Le chemin est relatif à la racine du projet (où Maven exécute les tests)
    environment = new DockerComposeContainer<>(new File("../docker-compose.test.yml"))
      .withExposedService("postgres", 5432);
    environment.start();
  }

  /**
   * Configure les propriétés Spring Boot avec les ports dynamiques des conteneurs.
   * INDISPENSABLE : sans cette méthode, l'application essaierait de se connecter
   * aux ports par défaut (5432, 9092, etc.) au lieu des ports dynamiques.
   */
  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    // PostgreSQL : récupère le port dynamique et configure la datasource
    var postgresHost = environment.getServiceHost("postgres", 5432);
    var postgresPort = environment.getServicePort("postgres", 5432);
    registry.add("spring.datasource.url",
      () -> String.format("jdbc:postgresql://%s:%d/hrconnect?currentSchema=employee", postgresHost, postgresPort));


  }

  // ==================== Beans injectés ====================

  @LocalServerPort
  protected int port;

  @Autowired
  private TestRestTemplate restTemplate;

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
      .nom("Alice Dupont")
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

    // When
    ResponseEntity<EmployeeDTO> response = restTemplate.postForEntity(
      "http://localhost:" + port + "/api/employees",
      employeeDTO,
      EmployeeDTO.class
    );

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getReference()).isEqualTo("E001");
    assertThat(response.getBody().getNom()).isEqualTo("Alice Dupont");

    // Vérifier en base
    Employee saved = employeeRepository.findByReference("E001").orElseThrow();
    assertThat(saved.getNom()).isEqualTo("Alice Dupont");
  }

  @Test
  void shouldGetEmployeeById() {
    // Given
    Employee employee = Employee.builder()
      .reference("E002")
      .nom("Bob Martin")
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

    // When
    ResponseEntity<EmployeeDTO> response = restTemplate.getForEntity(
      "http://localhost:" + port + "/api/employees/E002",
      EmployeeDTO.class
    );

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getReference()).isEqualTo("E002");
    assertThat(response.getBody().getNom()).isEqualTo("Bob Martin");
  }

  @Test
  void shouldReturnNotFoundForNonExistentEmployee() {
    // When
    ResponseEntity<EmployeeDTO> response = restTemplate.getForEntity(
      "http://localhost:" + port + "/api/employees/E999",
      EmployeeDTO.class
    );

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }
}

