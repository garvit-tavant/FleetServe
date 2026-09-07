package com.example.backend.ExecutionService.repository;

import com.example.backend.ExecutionService.entity.WorkOrderLabour;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkOrderLabourRepository extends JpaRepository<WorkOrderLabour, Long> {

}
