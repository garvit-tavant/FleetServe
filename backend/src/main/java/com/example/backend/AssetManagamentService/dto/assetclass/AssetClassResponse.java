package com.example.backend.AssetManagamentService.dto.assetclass;

public class AssetClassResponse {

    private Long id;

    private String code;

    private String description;

    // getters setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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