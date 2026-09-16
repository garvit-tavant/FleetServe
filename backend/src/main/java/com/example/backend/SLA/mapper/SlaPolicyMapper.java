package com.example.backend.SLA.mapper;

import org.springframework.stereotype.Component;

import com.example.backend.SLA.dto.CreateSlaPolicyRequest;
import com.example.backend.SLA.dto.SlaPolicyResponse;
import com.example.backend.SLA.entity.SlaPolicy;

@Component
public class SlaPolicyMapper {

    public SlaPolicy toEntity(CreateSlaPolicyRequest request) {

        if (request == null) {
            return null;
        }

        SlaPolicy policy = new SlaPolicy();

        policy.setPriority(request.getPriority());
        policy.setResponseTargetMinutes(
                request.getResponseTargetMinutes());
        policy.setResolutionTargetMinutes(
                request.getResolutionTargetMinutes());
        policy.setCalendarBasis(
                request.getCalendarBasis());
        policy.setEffectiveFrom(
                request.getEffectiveFrom());
        policy.setEffectiveTo(
                request.getEffectiveTo());

        return policy;
    }

    public SlaPolicyResponse toResponse(
            SlaPolicy policy) {

        if (policy == null) {
            return null;
        }

        SlaPolicyResponse response =
                new SlaPolicyResponse();

        response.setId(policy.getId());
        response.setPriority(policy.getPriority());
        response.setResponseTargetMinutes(
                policy.getResponseTargetMinutes());
        response.setResolutionTargetMinutes(
                policy.getResolutionTargetMinutes());
        response.setCalendarBasis(
                policy.getCalendarBasis());
        response.setEffectiveFrom(
                policy.getEffectiveFrom());
        response.setEffectiveTo(
                policy.getEffectiveTo());

        return response;
    }
}