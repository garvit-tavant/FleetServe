package com.example.backend.CapacityAndSchedulingService.mapper;

import com.example.backend.CapacityAndSchedulingService.dto.bay.BayResponse;
import com.example.backend.CapacityAndSchedulingService.dto.bayCapability.BayCapabilityResponse;
import com.example.backend.CapacityAndSchedulingService.dto.capability.CapabilityResponse;
import com.example.backend.CapacityAndSchedulingService.dto.depot.DepotResponse;
import com.example.backend.CapacityAndSchedulingService.dto.skill.SkillResponse;
import com.example.backend.CapacityAndSchedulingService.dto.technician.TechnicianResponse;
import com.example.backend.CapacityAndSchedulingService.dto.technicianskill.TechnicianSkillResponse;
import com.example.backend.CapacityAndSchedulingService.dto.workshop.WorkshopResponse;
import com.example.backend.CapacityAndSchedulingService.entity.*;

import org.springframework.stereotype.Component;

@Component
public class CapacitySchedulingMapper {

    public DepotResponse toDepotResponse(
            Depot depot
    ) {

        DepotResponse response =
                new DepotResponse();

        response.setId(depot.getId());
        response.setCode(depot.getCode());
        response.setRegion(depot.getRegion());
        response.setActive(depot.getActive());

        return response;
    }

    public WorkshopResponse toWorkshopResponse(
            Workshop workshop
    ) {

        WorkshopResponse response =
                new WorkshopResponse();

        response.setId(workshop.getId());
        response.setCode(workshop.getCode());

        if (workshop.getDepot() != null) {
            response.setDepotId(
                    workshop.getDepot().getId()
            );
        }

        response.setTimeZone(
                workshop.getTimeZone()
        );

        response.setActive(
                workshop.getActive()
        );

        return response;
    }

    public BayResponse toBayResponse(
            Bay bay
    ) {

        BayResponse response =
                new BayResponse();

        response.setId(bay.getId());

        if (bay.getWorkshop() != null) {
            response.setWorkshopId(
                    bay.getWorkshop().getId()
            );
        }

        response.setBayCode(
                bay.getBayCode()
        );

        response.setActive(
                bay.getIsActive()
        );

        return response;
    }

    public CapabilityResponse toCapabilityResponse(
            Capability capability
    ) {

        CapabilityResponse response =
                new CapabilityResponse();

        response.setCapabilityCode(
                capability.getCapabilityCode()
        );

        response.setName(
                capability.getName()
        );

        response.setDescription(
                capability.getDescription()
        );

        return response;
    }

    public BayCapabilityResponse toBayCapabilityResponse(
            BayCapability bayCapability
    ) {
        BayCapabilityResponse response =
                new BayCapabilityResponse();

        response.setId(bayCapability.getId());

        if (bayCapability.getBay() != null) {
            response.setBayId(
                    bayCapability.getBay().getId()
            );
        }

        if (bayCapability.getCapability() != null) {
            response.setCapabilityCode(
                    bayCapability
                            .getCapability()
                            .getCapabilityCode()
            );

            response.setCapabilityName(
                    bayCapability
                            .getCapability()
                            .getName()
            );
        }

        return response;
    }

    public SkillResponse toSkillResponse(
            Skill skill
    ) {

        SkillResponse response =
                new SkillResponse();

        response.setSkillCode(
                skill.getSkillCode()
        );

        response.setName(
                skill.getName()
        );

        response.setDescription(
                skill.getDescription()
        );

        response.setTime(
                skill.getTime()
        );

        return response;
    }

    public TechnicianResponse toTechnicianResponse(
            Technician technician
    ) {

        TechnicianResponse response =
                new TechnicianResponse();

        response.setId(
                technician.getId()
        );

        response.setAppUserId(
                technician.getAppUser().getId()
        );

        response.setWorkshopId(
                technician.getWorkshop().getId()
        );

        response.setWorkshopCode(
                technician.getWorkshop().getCode()
        );

        response.setHourlyRate(
                technician.getHourlyRate()
        );

        response.setActive(
                technician.getActive()
        );

        return response;
    }

    public TechnicianSkillResponse toTechnicianSkillResponse(
            TechnicianSkill technicianSkill
    ) {

        TechnicianSkillResponse response =
                new TechnicianSkillResponse();

        response.setId(
                technicianSkill.getId()
        );

        response.setTechnicianId(
                technicianSkill
                        .getTechnician()
                        .getId()
        );

        response.setSkillCode(
                technicianSkill
                        .getSkill()
                        .getSkillCode()
        );

        response.setSkillName(
                technicianSkill
                        .getSkill()
                        .getName()
        );

        response.setValidFrom(
                technicianSkill.getValidFrom()
        );

        response.setValidTo(
                technicianSkill.getValidTo()
        );

        return response;
    }
}