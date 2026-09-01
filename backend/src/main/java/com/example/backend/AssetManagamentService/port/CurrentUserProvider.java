package com.example.backend.AssetManagamentService.port;
// it takes the jwt and gives us user id, so that we can recieve
public interface CurrentUserProvider {

    Long getCurrentUserId();
}