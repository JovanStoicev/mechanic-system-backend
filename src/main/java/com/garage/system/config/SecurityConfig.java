package com.garage.system.config;

import com.garage.system.security.JwtAuthFilter;
import com.garage.system.security.JwtService;
import com.garage.system.mechanic.MechanicRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            MechanicRepository mechanicRepository,
            Environment env
    ) throws Exception {

        String originsProp = env.getProperty("app.cors.allowedOrigins", "http://localhost:5173");
        List<String> allowedOrigins = List.of(originsProp.split(","));

        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) ->
                        response.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized"))
                .accessDeniedHandler((request, response, exception) ->
                        response.sendError(HttpStatus.FORBIDDEN.value(), "Forbidden")));

        http.cors(cors -> cors.configurationSource(req -> {
            var cfg = new CorsConfiguration();
            cfg.setAllowedOrigins(allowedOrigins);
            cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
            cfg.setAllowedHeaders(List.of("*"));
            cfg.setAllowCredentials(true);
            return cfg;
        }));

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/login", "/api/auth/register/customer").permitAll()
                .requestMatchers("/api/boss/**").hasAuthority("ROLE_BOSS")
                .requestMatchers("/api/me/**").hasAuthority("ROLE_MECHANIC")
                .requestMatchers("/api/customer/**").hasAuthority("ROLE_CUSTOMER")
                .anyRequest().authenticated()
        );

        http.addFilterBefore(new JwtAuthFilter(jwtService, mechanicRepository), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
