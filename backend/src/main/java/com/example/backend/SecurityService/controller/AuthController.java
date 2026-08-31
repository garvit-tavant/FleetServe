package com.example.backend.SecurityService.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

import com.example.backend.SecurityService.dto.AuthResponse;
import com.example.backend.SecurityService.dto.LoginRequest;
import com.example.backend.SecurityService.dto.RegisterRequest;
import com.example.backend.SecurityService.service.AuthService;
import com.example.backend.SecurityService.security.JwtService;

import jakarta.validation.Valid;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService){
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register (@Valid @RequestBody RegisterRequest request){
      
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request){

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);

    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping(@RequestHeader("Authorization") String authHeader) {

        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid Authorization header");
        }
        String token = authHeader.replace("Bearer ", "");

        if (!jwtService.isTokenValid(token)) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(token);
        }
       

        return ResponseEntity.ok("Pong " + jwtService.extractUsername(token) + " " + jwtService.extractRoles(token).stream().collect(Collectors.joining(", ")));
    }

}
