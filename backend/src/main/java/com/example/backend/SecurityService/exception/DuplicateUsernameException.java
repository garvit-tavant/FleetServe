package com.example.backend.SecurityService.exception;

public class DuplicateUsernameException extends RuntimeException {
    
    public DuplicateUsernameException(String message){
        super(message);
    } 
    
}
