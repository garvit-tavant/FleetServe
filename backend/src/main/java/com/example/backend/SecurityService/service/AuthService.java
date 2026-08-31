package com.example.backend.SecurityService.service;

import com.example.backend.SecurityService.dto.AuthResponse;
import com.example.backend.SecurityService.dto.LoginRequest;
import com.example.backend.SecurityService.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    
}
