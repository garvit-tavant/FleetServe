package com.example.backend.SecurityService.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.backend.SecurityService.dto.AppUserPrincipal;
import com.example.backend.SecurityService.service.impl.AppUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final AppUserDetailsService appUserDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService , AppUserDetailsService appUserDetailsService) {
        this.jwtService = jwtService;
        this.appUserDetailsService = appUserDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/auth/"); // skip JWT filter entirely for auth endpoints
    }

    @Override
    protected void doFilterInternal (HttpServletRequest request , HttpServletResponse response , FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization"); // auth token bearer

        if(header == null || !header.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
            return ;    
        }

        String token = header.substring(7); // jwt token

        if (!jwtService.isTokenValid(token)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token");
            return;
        }

        // checking for validity
        if (SecurityContextHolder.getContext().getAuthentication() == null){ 
            // Claims claims = jwtService.parseClaims(token);
            String username = jwtService.extractUsername(token);
            Collection<String> roleStrings = jwtService.extractRoles(token);
            Set<GrantedAuthority> authorities = roleStrings.stream()
                                                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                                            .collect(Collectors.toSet());

                                // System.out.println("====================");
                                // authorities.stream().forEach(role -> System.out.println("Role from JWT: " + role.getAuthority()));
                                // System.out.println("====================");

            AppUserPrincipal principal = appUserDetailsService.loadUserByUsername(username, authorities);
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(principal, null, authorities);  
            SecurityContextHolder.getContext().setAuthentication(authToken);
           
        }

        filterChain.doFilter(request, response);

    }
}


