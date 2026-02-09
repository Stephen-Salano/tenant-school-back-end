package io.psyllabus.platform.common.config;

import io.psyllabus.platform.auth.enums.UserType;
import io.psyllabus.platform.common.filter.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final Environment environment;
    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;

    // public endpoints
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/forgot-password/*"
    };

    private static final String[] DEV_TEST_ENDPOINTS = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/h2-console/**",
            "/actuator/**"  // Full actuator access in dev
    };

    // Prod: Prometheus will scrape via internal network, not public HTTP
    private static final String[] PROD_ACTUATOR_ENDPOINTS = {
            "/actuator/health",     // Basic liveness
            "/actuator/prometheus"  // Metrics (but restricted by network/IP)
    };


    @Bean
    public AuthenticationProvider authenticationProvider (){
        // Connect the auth provider to our custom logic for finding users
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder()); // password hashing algorithm to use (BCrypt)
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws  Exception{
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain (HttpSecurity httpSecurity) throws Exception{
        // Check currently active profile
        boolean isDevOrTest = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("dev") || p.equalsIgnoreCase("test"));

        // create a list of endpoints that should always be public
        List<String> permitList = new ArrayList<>(List.of(PUBLIC_ENDPOINTS));
        // Conditionally permit
        if (isDevOrTest){
            permitList.addAll(List.of(DEV_TEST_ENDPOINTS));
            httpSecurity
                    .csrf(AbstractHttpConfigurer::disable).cors(cors -> cors.configurationSource(request -> {
                        if (request.getRequestURI().startsWith("/h2-console")){
                            return new CorsConfiguration().applyPermitDefaultValues();
                        }
                        return corsConfigurationSource.getCorsConfiguration(request);
                    })).headers(
                            headers -> headers
                                    .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                                    .httpStrictTransportSecurity(HeadersConfigurer.HstsConfig::disable)
                    );
        } else {
            // Prod: Only basic health check is public
            permitList.add("/actuator/health");
            // Prometheus scrapes /actuator/prometheus but via internal IP
            httpSecurity.cors(
                    cors -> cors.configurationSource(corsConfigurationSource)
            );
        }
        httpSecurity
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(permitList.toArray(new String[0])).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole(UserType.ADMIN.toString())
                        .requestMatchers("/api/v1/staff/**").hasRole(UserType.STAFF.toString())
                        .requestMatchers("/api/v1/student/**").hasRole(UserType.STUDENT.toString())
                        .requestMatchers("/api/v1/parent/**").hasRole(UserType.PARENT.toString())
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    /*
     TODO: configure the CSRF repository to use cookies instead of the session (e.g., CookieCsrfTokenRepository)
       so that the CSRF token is stateless as well.

     */

}
