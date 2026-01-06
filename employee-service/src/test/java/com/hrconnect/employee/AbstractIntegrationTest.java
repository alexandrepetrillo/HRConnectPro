package com.hrconnect.employee;

import com.hrconnect.employee.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

/**
 * Classe abstraite de base pour les tests d'intégration.
 * Configure les conteneurs Testcontainers (PostgreSQL, Kafka) et fournit
 * des utilitaires communs pour l'authentification JWT.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

  // ==================== Conteneurs Testcontainers ====================

  protected static PostgreSQLContainer<?> postgres;
  protected static KafkaContainer kafka;

  static {
    postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine")).withDatabaseName("testdb").withUsername("test").withPassword("test");
    postgres.start();

    kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
    kafka.start();
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    // Désactiver LDAP et utiliser l'authentification in-memory pour les tests
    registry.add("ldap.enabled", () -> "false");
  }

  // ==================== Beans injectés ====================

  @LocalServerPort
  protected int port;

  @Autowired
  protected TestRestTemplate restTemplate;

  @Autowired
  protected JwtTokenProvider jwtTokenProvider;

  // ==================== Variables utilitaires ====================

  protected String baseUrl;

  // ==================== Utilisateurs de test (définis dans SecurityConfig) ====================

  protected static final String HR_USER = "hr_user";
  protected static final String HR_PASSWORD = "password";
  protected static final String ADMIN_USER = "admin";
  protected static final String ADMIN_PASSWORD = "admin";

  // ==================== Setup ====================

  @BeforeEach
  void setUpBase() {
    baseUrl = "http://localhost:" + port;
  }

  // ==================== Méthodes utilitaires ====================

  /**
   * Crée des headers HTTP avec un token JWT pour un utilisateur avec le rôle HR.
   */
  protected HttpHeaders createHrAuthHeaders() {
    return createAuthHeadersWithRole("ROLE_HR");
  }

  /**
   * Crée des headers HTTP avec un token JWT pour un utilisateur avec le rôle ADMIN.
   */
  protected HttpHeaders createAdminAuthHeaders() {
    return createAuthHeadersWithRole("ROLE_ADMIN");
  }

  /**
   * Crée des headers HTTP avec un token JWT pour un utilisateur avec le rôle spécifié.
   */
  protected HttpHeaders createAuthHeadersWithRole(String role) {
    String token = generateTokenWithRole("testuser", role);
    return createHeadersWithToken(token);
  }

  /**
   * Crée des headers HTTP avec un token JWT pour un utilisateur et rôle spécifiés.
   */
  protected HttpHeaders createAuthHeaders(String username, String role) {
    String token = generateTokenWithRole(username, role);
    return createHeadersWithToken(token);
  }

  /**
   * Crée des headers HTTP avec le token JWT spécifié.
   */
  protected HttpHeaders createHeadersWithToken(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  /**
   * Crée des headers HTTP sans authentification.
   */
  protected HttpHeaders createHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  /**
   * Génère un token JWT pour un utilisateur avec le rôle spécifié.
   */
  protected String generateTokenWithRole(String username, String role) {
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username, null, List.of(new SimpleGrantedAuthority(role)));
    return jwtTokenProvider.generateToken(authentication);
  }

  /**
   * Génère un token JWT pour un utilisateur avec plusieurs rôles.
   */
  protected String generateTokenWithRoles(String username, List<String> roles) {
    List<SimpleGrantedAuthority> authorities = roles.stream().map(SimpleGrantedAuthority::new).toList();
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);
    return jwtTokenProvider.generateToken(authentication);
  }

  /**
   * Construit l'URL complète pour un endpoint.
   */
  protected String url(String path) {
    return baseUrl + path;
  }
}

