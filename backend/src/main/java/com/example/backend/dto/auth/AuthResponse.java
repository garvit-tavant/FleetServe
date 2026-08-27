package com.example.backend.dto.auth;

import java.util.Set;

public class AuthResponse {
    private Long userID;
    private String username;
    private Set<String> roles;
    private String token;

    public AuthResponse(){}

    public AuthResponse(Long userID, String username, Set<String> roles, String token) {
        this.userID = userID;
        this.username = username;
        this.roles = roles;
        this.token = token;
    }

    // ---------------------------------

    
    public Long getUserID() {
        return userID;
    }

    public void setUserID(Long userID) {
        this.userID = userID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}
