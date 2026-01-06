package com.hrconnect.employee.infrastructure.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;

/**
 * Configuration Spring Security avec LDAP + JWT
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${ldap.enabled:false}")
    private boolean ldapEnabled;

    @Value("${ldap.url:ldap://localhost:389}")
    private String ldapUrl;

    @Value("${ldap.base-dn:dc=hrconnect,dc=local}")
    private String ldapBaseDn;

    @Value("${ldap.user-dn-pattern:uid={0},ou=users}")
    private String ldapUserDnPattern;

    @Value("${ldap.manager-dn:}")
    private String ldapManagerDn;

    @Value("${ldap.manager-password:}")
    private String ldapManagerPassword;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Endpoints publics (doivent être déclarés ici car avant le filtre JWT)
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html", "/api-docs/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Actuator complet nécessite ADMIN
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                // Tout le reste est géré par les annotations @PreAuthorize sur les contrôleurs
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"Unauthorized\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"Access Denied\"}");
                })
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        if (ldapEnabled) {
            try {
                // Configuration LDAP standard (OpenLDAP)
                DefaultSpringSecurityContextSource contextSource =
                    new DefaultSpringSecurityContextSource(ldapUrl + "/" + ldapBaseDn);

                if (StringUtils.hasText(ldapManagerDn) && StringUtils.hasText(ldapManagerPassword)) {
                    contextSource.setUserDn(ldapManagerDn);
                    contextSource.setPassword(ldapManagerPassword);
                }

                contextSource.afterPropertiesSet();

                // Authentificateur par bind LDAP
                BindAuthenticator authenticator = new BindAuthenticator(contextSource);

                // ✅ IMPORTANT : Pattern simple sans recherche préalable
                authenticator.setUserDnPatterns(new String[]{ldapUserDnPattern});

                // Populateur d'autorités (rôles) - optionnel, ne plante pas si les groupes n'existent pas
                String groupSearchBase = "ou=groups"; // relatif au base DN pour éviter les doublons
                DefaultLdapAuthoritiesPopulator authoritiesPopulator =
                    new DefaultLdapAuthoritiesPopulator(contextSource, groupSearchBase);
                authoritiesPopulator.setGroupRoleAttribute("cn");
                authoritiesPopulator.setGroupSearchFilter("(member={0})");
                authoritiesPopulator.setRolePrefix("ROLE_");
                authoritiesPopulator.setSearchSubtree(true);
                authoritiesPopulator.setConvertToUpperCase(true);

                // Ne pas planter si les groupes n'existent pas
                authoritiesPopulator.setIgnorePartialResultException(true);

                // Provider LDAP
                LdapAuthenticationProvider ldapProvider =
                    new LdapAuthenticationProvider(authenticator, authoritiesPopulator);

                // ✅ CRITIQUE : Créer un ProviderManager SANS parent pour éviter les retries
                ProviderManager providerManager = new ProviderManager(ldapProvider);
                providerManager.setEraseCredentialsAfterAuthentication(false);
                return providerManager;
            } catch (Exception e) {
                // Si LDAP échoue, fallback sur in-memory
                System.err.println("⚠️  LDAP configuration failed: " + e.getMessage());
                System.err.println("⚠️  Falling back to In-Memory authentication");
                return createInMemoryAuthenticationManager();
            }
        } else {
            return createInMemoryAuthenticationManager();
        }
    }

    private AuthenticationManager createInMemoryAuthenticationManager() {
        // Configuration in-memory pour les tests
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(false);

        // ✅ CRITIQUE : Créer un ProviderManager SANS parent pour éviter les retries
        ProviderManager providerManager = new ProviderManager(provider);
        providerManager.setEraseCredentialsAfterAuthentication(false);
        return providerManager;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();

        manager.createUser(User.withUsername("hr_user")
            .password(passwordEncoder().encode("password"))
            .roles("HR")
            .build());

        manager.createUser(User.withUsername("admin")
            .password(passwordEncoder().encode("admin"))
            .roles("ADMIN", "HR")
            .build());

        return manager;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
