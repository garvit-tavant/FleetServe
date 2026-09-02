package com.example.backend.CapacityAndSchedulingService.repository;

import com.example.backend.CapacityAndSchedulingService.entity.Technician;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TechnicianRepository
        extends JpaRepository<Technician, Long> {

    Optional<Technician> findByAppUser_Id(
            Long appUserId
    );

    boolean existsByAppUser_Id(
            Long appUserId
    );

    List<Technician> findByWorkshop_Id(
            Long workshopId
    );

    List<Technician> findByWorkshop_IdAndActiveTrue(
            Long workshopId
    );
}