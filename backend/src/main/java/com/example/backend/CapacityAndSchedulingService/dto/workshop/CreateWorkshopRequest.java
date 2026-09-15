package com.example.backend.CapacityAndSchedulingService.dto.workshop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateWorkshopRequest {

    @NotBlank
    @Size(max = 30)
    private String code;

    @NotNull
    private Long depotId;

    @NotBlank
    @Size(max = 100)
    private String timeZone;

    public CreateWorkshopRequest() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getDepotId() {
        return depotId;
    }

    public void setDepotId(Long depotId) {
        this.depotId = depotId;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }
}