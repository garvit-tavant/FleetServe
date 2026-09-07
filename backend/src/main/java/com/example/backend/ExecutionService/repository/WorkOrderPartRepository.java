package com.example.backend.ExecutionService.repository;

import com.example.backend.ExecutionService.entity.WorkOrderPart;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkOrderPartRepository extends JpaRepository<WorkOrderPart, Long> {

} 
