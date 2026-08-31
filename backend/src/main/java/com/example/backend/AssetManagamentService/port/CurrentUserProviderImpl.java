package com.example.backend.AssetManagamentService.port;

import org.springframework.stereotype.Component;

@Component
public class CurrentUserProviderImpl
        implements CurrentUserProvider {

    @Override
    public Long getCurrentUserId() {
        return 1L;
    }
}
