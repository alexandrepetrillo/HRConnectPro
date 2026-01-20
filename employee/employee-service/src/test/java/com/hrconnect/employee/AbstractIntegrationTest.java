package com.hrconnect.employee;

import com.hrconnect.socle.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;
import java.util.List;

/**
 * Classe abstraite de base pour les tests d'intégration.
 * Configure les conteneurs Testcontainers (PostgreSQL, Kafka) et fournit
 * des utilitaires communs pour l'authentification JWT.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

  // ==================== Conteneurs Testcontainers ====================

  protected static DockerComposeContainer<?> environment;

  static {
    // Charge docker-compose.test.yml qui contient la configuration complète pour les tests
    // avec ports dynamiques pour éviter les conflits avec l'environnement de dev local
    environment = new DockerComposeContainer<>(new File("../docker-compose.test.yml"))
      .withExposedService("postgres", 5432)
      .withExposedService("kafka", 9092)
      .withExposedService("ldap", 389);
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

    // Kafka : récupère le port dynamique et configure le bootstrap server
    String kafkaHost = environment.getServiceHost("kafka", 9092);
    Integer kafkaPort = environment.getServicePort("kafka", 9092);
    registry.add("spring.kafka.bootstrap-servers",
      () -> String.format("%s:%d", kafkaHost, kafkaPort));

    // LDAP : récupère le port dynamique (même si ldap.enabled=false en test)
    String ldapHost = environment.getServiceHost("ldap", 389);
    Integer ldapPort = environment.getServicePort("ldap", 389);
    registry.add("ldap.url", () -> String.format("ldap://%s:%d", ldapHost, ldapPort));
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

