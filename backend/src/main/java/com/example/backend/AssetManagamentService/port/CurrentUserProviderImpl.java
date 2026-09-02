package com.example.backend.AssetManagamentService.port;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.backend.SecurityService.dto.AppUserPrincipal;
import com.example.backend.SecurityService.entity.AppUser;
import com.example.backend.SecurityService.repository.UserRepository;

@Component
public class CurrentUserProviderImpl implements CurrentUserProvider {

    private final UserRepository userRepository;

    public CurrentUserProviderImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user in security context");
        }

        String username = ((AppUserPrincipal) auth.getPrincipal()).getUsername();  // auth.getPrincipal provide our class AppUserPrincipal 

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in DB: " + username));

        return user.getId();
    }
}
