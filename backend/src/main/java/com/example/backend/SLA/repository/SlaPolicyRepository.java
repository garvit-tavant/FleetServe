package com.example.backend.SLA.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.SLA.entity.SlaPolicy;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy,Long> {
    
    
}
