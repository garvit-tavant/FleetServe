package com.example.backend.SecurityService.exception;

public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException(String message){
        super(message);
    }
    
}
