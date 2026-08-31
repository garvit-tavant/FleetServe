package com.example.backend.AssetManagamentService.dto.assetclass;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateAssetClassRequest {
    @NotBlank(message = "Asset-class code is required")
    @Size(
    max = 50,
    message = "Asset-class code cannot exceed 50 characters")
    private String code;
    @NotBlank(message = "Asset-class description is required")
    @Size(
    max = 255,
    message = "Asset-class description cannot exceed 255 characters")
    private String description;
    // getters setters

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}