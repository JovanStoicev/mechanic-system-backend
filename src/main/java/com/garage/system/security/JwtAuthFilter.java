package com.garage.system.security;

import com.garage.system.mechanic.MechanicRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final MechanicRepository mechanicRepository;

    public JwtAuthFilter(JwtService jwtService, MechanicRepository mechanicRepository) {
        this.jwtService = jwtService;
        this.mechanicRepository = mechanicRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = auth.substring("Bearer ".length()).trim();
        try {
            var claims = jwtService.parse(token);
            if ("ROLE_MECHANIC".equals(claims.role()) && mechanicRepository.findByUser_Id(claims.userId())
                    .map(mechanic -> !mechanic.isActive()).orElse(true)) {
                throw new IllegalStateException("Mechanic account is inactive");
            }

            var authorities = List.of(new SimpleGrantedAuthority(claims.role()));
            var authentication = new UsernamePasswordAuthenticationToken(
                    claims.userId(),
                    null,
                    authorities
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception ex) {
            // invalid token → ignore and continue (will be blocked by security rules)
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
