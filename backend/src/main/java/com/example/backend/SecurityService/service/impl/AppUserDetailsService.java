package com.example.backend.SecurityService.service.impl;


import java.util.Collection;


import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;


import com.example.backend.SecurityService.dto.AppUserPrincipal;

@Service
public class AppUserDetailsService {
    

    public AppUserPrincipal loadUserByUsername(String username , Collection<? extends GrantedAuthority> authorities) {
      //  AppUser user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("No such user: "+ username) );
        return new AppUserPrincipal(username , authorities);
    }

}
