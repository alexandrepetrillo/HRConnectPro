package com.hrconnect.employee.presentation.controller;

import com.hrconnect.socle.security.JwtTokenProvider;
import com.hrconnect.employee.presentation.dto.AuthErrorResponse;
import com.hrconnect.employee.presentation.dto.JwtResponse;
import com.hrconnect.employee.presentation.dto.LoginRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Contrôleur pour l'authentification et la génération de tokens JWT
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "API d'authentification (LDAP + JWT)")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final MeterRegistry meterRegistry;

    private Counter authSuccessCounter;
    private Counter authFailureCounter;

    @PostConstruct
    public void initMetrics() {
        authSuccessCounter = Counter.builder("auth_login_total")
                .tag("result", "success")
                .description("Nombre total d'authentifications réussies")
                .register(meterRegistry);

        authFailureCounter = Counter.builder("auth_login_total")
                .tag("result", "failure")
                .description("Nombre total d'authentifications échouées")
                .register(meterRegistry);
    }

    @PostMapping("/login")
    @PermitAll
    @Operation(summary = "Authentification et génération de token JWT")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for user: {}", loginRequest.getUsername());

        try {
            // Authentification via LDAP (ou in-memory pour les tests)
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Génération du token JWT
            String jwt = tokenProvider.generateToken(authentication);

            // Extraction des rôles
            List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

            log.info("User {} authenticated successfully with roles: {}", loginRequest.getUsername(), roles);

            // Incrémenter le compteur de succès
            authSuccessCounter.increment();

            return ResponseEntity.ok(JwtResponse.builder()
                .token(jwt)
                .username(authentication.getName())
                .roles(roles)
                .build());

        } catch (BadCredentialsException e) {
            // Échec d'authentification - credentials invalides
            log.warn("Authentication failed for user {}: Invalid credentials", loginRequest.getUsername());
            authFailureCounter.increment();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthErrorResponse("Invalid username or password"));
        } catch (AuthenticationException e) {
            // Toute autre erreur d'authentification Spring Security
            log.warn("Authentication failed for user {}: {}", loginRequest.getUsername(), e.getMessage());
            authFailureCounter.increment();

            // Si l'erreur contient "No Such Object", c'est que LDAP n'est pas initialisé
            if (e.getMessage().contains("No Such Object") || e.getMessage().contains("error code 32")) {
                log.error("LDAP structure not initialized! Ensure the container bootstrap LDIF is loaded.");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new AuthErrorResponse("LDAP not initialized. Contact administrator."));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthErrorResponse("Authentication failed"));
        } catch (Exception e) {
            // Filet de sécurité pour toute erreur non prévue
            log.error("Unexpected error during authentication for user {}: {}", loginRequest.getUsername(), e.getMessage());
            authFailureCounter.increment();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthErrorResponse("An error occurred during authentication"));
        }
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Récupérer les informations de l'utilisateur connecté")
    public ResponseEntity<JwtResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        List<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());

        return ResponseEntity.ok(JwtResponse.builder()
            .username(authentication.getName())
            .roles(roles)
            .build());
    }
}
