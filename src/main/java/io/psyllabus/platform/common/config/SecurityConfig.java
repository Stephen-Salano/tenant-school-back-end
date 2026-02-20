package io.psyllabus.platform.common.config;

import io.psyllabus.platform.auth.enums.UserType;
import io.psyllabus.platform.auth.repository.UserRepository;
import io.psyllabus.platform.common.filter.JwtAuthFilter;
import io.psyllabus.platform.common.util.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final Environment environment;
    private final JwtAuthFilter jwtAuthFilter;
    private final UserRepository userRepository;
    private final CorsConfigurationSource corsConfigurationSource;

    // Public endpoints (no JWT required)
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/forgot-password/**",
            "/api/schools/onboard",
            "/error"
    };

    // Endpoints allowed only in Dev/Test profiles
    private static final String[] DEV_TEST_ENDPOINTS = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/h2-console/**",
            "/actuator/**"
    };

    // Endpoints allowed in Prod (restricted)
    private static final String[] PROD_ACTUATOR_ENDPOINTS = {
            "/actuator/health",
            "/actuator/prometheus"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Check currently active profile
        boolean isDevOrTest = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("dev") || p.equalsIgnoreCase("test"));

        // Build dynamic permit list
        List<String> permitList = new ArrayList<>(List.of(PUBLIC_ENDPOINTS));

        if (isDevOrTest) {
            permitList.addAll(List.of(DEV_TEST_ENDPOINTS));
        } else {
            permitList.addAll(List.of(PROD_ACTUATOR_ENDPOINTS));
        }

        http
            // Stateless REST — no CSRF, no sessions
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // CORS Configuration
            .cors(cors -> {
                if (isDevOrTest) {
                    // In dev, allow H2 console specifically if needed, otherwise use source
                    cors.configurationSource(request -> {
                        if (request.getRequestURI().startsWith("/h2-console")) {
                            return new CorsConfiguration().applyPermitDefaultValues();
                        }
                        return corsConfigurationSource.getCorsConfiguration(request);
                    });
                } else {
                    cors.configurationSource(corsConfigurationSource);
                }
            })

            // Headers Configuration
            .headers(headers -> {
                if (isDevOrTest) {
                    headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin) // Allow H2 console iframe
                           .httpStrictTransportSecurity(HeadersConfigurer.HstsConfig::disable);
                }
                // In prod, SecurityHeadersConfig filter handles strict headers (CSP, HSTS)
            })

            // Route-level access control
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(permitList.toArray(new String[0])).permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole(UserType.ADMIN.name())
                .requestMatchers("/api/v1/staff/**").hasRole(UserType.STAFF.name())
                .requestMatchers("/api/v1/student/**").hasRole(UserType.STUDENT.name())
                .requestMatchers("/api/v1/parent/**").hasRole(UserType.PARENT.name())
                .anyRequest().authenticated()
            )

            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt with strength=12.
     * Injected into PasswordServiceImpl.
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * UserDetailsService used by Spring's AuthenticationProvider.
     * Loads user by identifier (username/email) within the current tenant.
     *
     * TenantContext is set before this is called (TenantFilter runs first).
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return identifier -> {
            UUID tenantId = TenantContext.getCurrentTenantId();
            if (tenantId == null) {
                log.warn("UserDetailsService called without tenant context");
                throw new UsernameNotFoundException("Tenant context not available");
            }
            return userRepository.findByIdentifierAndTenantId(identifier, tenantId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier));
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
