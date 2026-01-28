package com.hrconnect.socle.security;

import com.hrconnect.socle.security.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;

/**
 * Auto-configuration Spring Security pour le socle HRConnect.
 *
 * <p>Configure automatiquement la sécurité avec support LDAP et JWT :</p>
 * <ul>
 *   <li>Authentification LDAP si {@code ldap.enabled=true}</li>
 *   <li>Fallback sur authentification in-memory sinon</li>
 *   <li>Validation JWT pour les requêtes authentifiées</li>
 *   <li>Endpoints publics : login, swagger, actuator health</li>
 * </ul>
 *
 * <p>Configuration via application.yml :</p>
 * <pre>
 * ldap:
 *   enabled: true
 *   url: ldap://localhost:389
 *   base-dn: dc=hrconnect,dc=local
 *   user-dn-pattern: uid={0},ou=users
 *   manager-dn: cn=admin,dc=hrconnect,dc=local
 *   manager-password: admin
 * </pre>
 *
 * <p>Cette configuration peut être surchargée par les microservices en créant
 * leurs propres beans SecurityFilterChain ou AuthenticationManager.</p>
 */
@AutoConfiguration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true)
@ConditionalOnClass(SecurityFilterChain.class)
@Slf4j
public class SocleSecurityAutoConfiguration {

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

    @Value("${ldap.group-search-base:ou=groups}")
    private String ldapGroupSearchBase;

    public SocleSecurityAutoConfiguration(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Configure la chaîne de filtres de sécurité par défaut.
     * <ul>
     *   <li>CSRF désactivé (API stateless)</li>
     *   <li>Session stateless</li>
     *   <li>Endpoints publics : /api/auth/login, swagger, actuator health</li>
     *   <li>Actuator complet : ADMIN seulement</li>
     *   <li>Tout le reste : authentifié</li>
     * </ul>
     */
    @Bean
    @ConditionalOnMissingBean
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
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
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

    /**
     * Configure l'AuthenticationManager selon le mode d'authentification.
     * Si LDAP est activé et disponible, utilise LDAP. Sinon, utilise in-memory.
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthenticationManager authenticationManager() {
        if (ldapEnabled) {
            try {
                return createLdapAuthenticationManager();
            } catch (Exception e) {
                log.warn("⚠️  LDAP configuration failed: {}", e.getMessage());
                log.warn("⚠️  Falling back to In-Memory authentication");
                return createInMemoryAuthenticationManager();
            }
        } else {
            log.info("LDAP disabled, using In-Memory authentication");
            return createInMemoryAuthenticationManager();
        }
    }

    /**
     * Crée un AuthenticationManager LDAP avec recherche de groupes.
     */
    private AuthenticationManager createLdapAuthenticationManager() {
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
        authenticator.setUserDnPatterns(new String[]{ldapUserDnPattern});

        // Populateur d'autorités (rôles)
        DefaultLdapAuthoritiesPopulator authoritiesPopulator =
                new DefaultLdapAuthoritiesPopulator(contextSource, ldapGroupSearchBase);
        authoritiesPopulator.setGroupRoleAttribute("cn");
        authoritiesPopulator.setGroupSearchFilter("(member={0})");
        authoritiesPopulator.setRolePrefix("ROLE_");
        authoritiesPopulator.setSearchSubtree(true);
        authoritiesPopulator.setConvertToUpperCase(true);
        authoritiesPopulator.setIgnorePartialResultException(true);

        // Provider LDAP
        LdapAuthenticationProvider ldapProvider =
                new LdapAuthenticationProvider(authenticator, authoritiesPopulator);

        // ✅ CRITIQUE : Créer un ProviderManager SANS parent pour éviter les retries
        ProviderManager providerManager = new ProviderManager(ldapProvider);
        providerManager.setEraseCredentialsAfterAuthentication(false);

        log.info("LDAP Authentication configured: url={}, baseDn={}", ldapUrl, ldapBaseDn);
        return providerManager;
    }

    /**
     * Crée un AuthenticationManager in-memory pour les tests/développement.
     */
    private AuthenticationManager createInMemoryAuthenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(inMemoryUserDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(false);

        ProviderManager providerManager = new ProviderManager(provider);
        providerManager.setEraseCredentialsAfterAuthentication(false);
        return providerManager;
    }

    /**
     * UserDetailsService in-memory pour les tests avec utilisateurs par défaut.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "ldap.enabled", havingValue = "false", matchIfMissing = true)
    public UserDetailsService inMemoryUserDetailsService() {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();

        // Chuck Norris - ADMIN : peut tout faire
        manager.createUser(User.withUsername("chuck")
                .password(passwordEncoder().encode("password"))
                .roles("ADMIN")
                .build());

        // Kevin - MANAGER : peut tout faire sauf supprimer
        manager.createUser(User.withUsername("kevin")
                .password(passwordEncoder().encode("password"))
                .roles("MANAGER")
                .build());

        // Sophie - USER : ne peut voir que ses propres infos via /me
        manager.createUser(User.withUsername("sophie")
                .password(passwordEncoder().encode("password"))
                .authorities("ROLE_USER")
                .build());

        log.info("In-Memory UserDetailsService configured with users: chuck (ADMIN), kevin (MANAGER), sophie (USER)");
        return manager;
    }

    /**
     * PasswordEncoder par défaut (BCrypt).
     */
    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
