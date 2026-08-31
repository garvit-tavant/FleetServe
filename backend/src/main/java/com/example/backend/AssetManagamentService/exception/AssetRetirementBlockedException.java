package com.example.backend.AssetManagamentService.exception;

import java.util.List;

public class AssetRetirementBlockedException extends RuntimeException {

    private final List<String> blockingRecords;

    public AssetRetirementBlockedException(
            String message,
            List<String> blockingRecords
    ) {
        super(message);
        this.blockingRecords = blockingRecords;
    }

    public List<String> getBlockingRecords() {
        return blockingRecords;
    }
}