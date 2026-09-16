package com.example.backend.ExecutionService.dto.workorder;

import java.util.List;

import com.example.backend.ExecutionService.dto.workorderlabour.WorkOrderLabourResponse;
import com.example.backend.ExecutionService.dto.workorderpart.WorkOrderPartResponse;

public class WorkOrderDetailsResponse
        extends WorkOrderResponse {

    private List<WorkOrderLabourResponse> labourEntries;

    private List<WorkOrderPartResponse> partsUsed;

    // getters setters


    public List<WorkOrderLabourResponse> getLabourEntries() {
        return labourEntries;
    }

    public void setLabourEntries(List<WorkOrderLabourResponse> labourEntries) {
        this.labourEntries = labourEntries;
    }

    public List<WorkOrderPartResponse> getPartsUsed() {
        return partsUsed;
    }

    public void setPartsUsed(List<WorkOrderPartResponse> partsUsed) {
        this.partsUsed = partsUsed;
    }
}