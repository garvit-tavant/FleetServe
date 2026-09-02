package com.example.backend.SecurityService.service.impl;

import com.example.backend.SecurityService.dto.AuthResponse;
import com.example.backend.SecurityService.dto.LoginRequest;
import com.example.backend.SecurityService.dto.RegisterRequest;
import com.example.backend.SecurityService.entity.AppUser;
import com.example.backend.SecurityService.entity.Role;
import com.example.backend.SecurityService.exception.AuthenticationFailedException;
import com.example.backend.SecurityService.exception.DuplicateUsernameException;
import com.example.backend.SecurityService.repository.RoleRepository;
import com.example.backend.SecurityService.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.SecurityService.security.JwtService;
import com.example.backend.SecurityService.service.AuthService;


import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;


@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private static final String DEFAULT_ROLE_CODE = "TECHNICIAN";

    public AuthServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }


    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        
        if(userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUsernameException("Username already exists" + request.getUsername());
        }
        
        // new register is by default given TECHNICIAN as role !!
        Role defaultRole = roleRepository.findByRoleCode(DEFAULT_ROLE_CODE).orElseThrow(()-> new IllegalStateException("default role not seeded: "+ DEFAULT_ROLE_CODE));
        
        AppUser user = new AppUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setIsActive(true);
        Set<Role> roles = new HashSet<>();
        roles.add(defaultRole);
        user.setRoles(roles);

        AppUser saved = userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername(), Set.of(DEFAULT_ROLE_CODE));  // single role
        return toAuthResponse(saved, token);
    }

    

    @Override
    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository.findByUsername(request.getUsername()).orElseThrow(()-> new AuthenticationFailedException("Invalid username or password"));
        
        if(!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())){
            throw new AuthenticationFailedException("Invalid username or password");
        }

        if(!user.getIsActive()){
            throw new AuthenticationFailedException("User account is inactive"); // checked if user is active
        }

        String token = jwtService.generateToken(user.getUsername(), user.getRoles().stream().map(Role::getRoleCode).collect(Collectors.toSet()));
        return toAuthResponse(user, token);
    }

    private AuthResponse toAuthResponse(AppUser user , String token){
        Set<String> roleCodes = user.getRoles().stream()
                                .map(Role::getRoleCode)
                                .collect(Collectors.toSet());
        
                return new AuthResponse(user.getId(),user.getUsername(),roleCodes,token);
    }
    
}
