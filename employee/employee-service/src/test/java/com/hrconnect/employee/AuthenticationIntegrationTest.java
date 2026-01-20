package com.hrconnect.employee;

import com.hrconnect.employee.presentation.dto.JwtResponse;
import com.hrconnect.employee.presentation.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration pour l'authentification JWT
 */
class AuthenticationIntegrationTest extends AbstractIntegrationTest {

    // ==================== Tests de Login ====================

    @Test
    void shouldLoginWithValidCredentials() {
        // Given - Utilisateur valide (défini dans SecurityConfig in-memory)
        LoginRequest loginRequest = new LoginRequest(HR_USER, HR_PASSWORD);

        HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, createHeaders());

        // When
        ResponseEntity<JwtResponse> response = restTemplate.exchange(
            url("/api/auth/login"),
            HttpMethod.POST,
            request,
            JwtResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isNotBlank();
        assertThat(response.getBody().getUsername()).isEqualTo(HR_USER);
        assertThat(response.getBody().getRoles()).contains("ROLE_MANAGER");
    }

    @Test
    void shouldReturnUnauthorizedWithInvalidPassword() {
        // Given - Mot de passe invalide
        LoginRequest loginRequest = new LoginRequest(HR_USER, "wrongpassword");

        HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, createHeaders());

        // When
        ResponseEntity<String> response = restTemplate.exchange(
            url("/api/auth/login"),
            HttpMethod.POST,
            request,
            String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturnUnauthorizedWithUnknownUser() {
        // Given - Utilisateur inconnu
        LoginRequest loginRequest = new LoginRequest("unknown.user", "somepassword");

        HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, createHeaders());

        // When
        ResponseEntity<String> response = restTemplate.exchange(
            url("/api/auth/login"),
            HttpMethod.POST,
            request,
            String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ==================== Tests de validation JWT ====================

    @Test
    void shouldValidateGeneratedToken() {
        // Given - Génération d'un token
        String token = generateTokenWithRole("testuser", "ROLE_MANAGER");

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);
        String username = jwtTokenProvider.getUsername(token);
        String roles = jwtTokenProvider.getRoles(token);

        // Then
        assertThat(isValid).isTrue();
        assertThat(username).isEqualTo("testuser");
        assertThat(roles).contains("ROLE_MANAGER");
    }

    @Test
    void shouldRejectInvalidToken() {
        // Given - Token invalide
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature";

        // When
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldRejectMalformedToken() {
        // Given - Token mal formé
        String malformedToken = "not-a-jwt-token";

        // When
        boolean isValid = jwtTokenProvider.validateToken(malformedToken);

        // Then
        assertThat(isValid).isFalse();
    }

    // ==================== Tests de l'endpoint /api/auth/me ====================

    @Test
    void shouldReturnCurrentUserInfo() {
        // Given - Token valide avec rôle MANAGER
        String token = generateTokenWithRole("manager.user", "ROLE_MANAGER");

        HttpEntity<Void> request = new HttpEntity<>(createHeadersWithToken(token));

        // When
        ResponseEntity<JwtResponse> response = restTemplate.exchange(
            url("/api/auth/me"),
            HttpMethod.GET,
            request,
            JwtResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo("manager.user");
        assertThat(response.getBody().getRoles()).contains("ROLE_MANAGER");
    }

    @Test
    void shouldReturnUnauthorizedForMeEndpointWithoutToken() {
        // When - Requête sans token
        ResponseEntity<String> response = restTemplate.getForEntity(
            url("/api/auth/me"),
            String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ==================== Tests des rôles ====================

    @Test
    void shouldAccessProtectedResourceWithHRRole() {
        // Given - Token avec rôle HR
        HttpEntity<Void> request = new HttpEntity<>(createHrAuthHeaders());

        // When - Accès à un endpoint protégé
        ResponseEntity<String> response = restTemplate.exchange(
            url("/api/employees"),
            HttpMethod.GET,
            request,
            String.class
        );

        // Then - Accès autorisé (200 ou autre code mais pas 401/403)
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldAccessProtectedResourceWithAdminRole() {
        // Given - Token avec rôle ADMIN
        HttpEntity<Void> request = new HttpEntity<>(createAdminAuthHeaders());

        // When - Accès à un endpoint protégé
        ResponseEntity<String> response = restTemplate.exchange(
            url("/api/employees"),
            HttpMethod.GET,
            request,
            String.class
        );

        // Then - Accès autorisé
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldDenyAccessWithUserRole() {
        // Given - Token avec rôle USER (non autorisé pour /api/employees)
        HttpEntity<Void> request = new HttpEntity<>(createAuthHeadersWithRole("ROLE_USER"));

        // When - Accès à un endpoint protégé
        ResponseEntity<String> response = restTemplate.exchange(
            url("/api/employees"),
            HttpMethod.GET,
            request,
            String.class
        );

        // Then - Accès refusé (403 Forbidden)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldDenyAccessWithoutToken() {
        // When - Accès sans token
        ResponseEntity<String> response = restTemplate.getForEntity(
            url("/api/employees"),
            String.class
        );

        // Then - Accès refusé (401 Unauthorized)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}

