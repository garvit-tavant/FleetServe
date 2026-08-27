package com.example.backend.repository;

import com.example.backend.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository  extends JpaRepository<AppUser,Long>{
    Optional<AppUser> findByUsername(String username);
    boolean existsByUsername(String username);
}
