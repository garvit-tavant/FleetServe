package com.example.backend.AssetManagamentService.service.impl;

import com.example.backend.AssetManagamentService.dto.assetclass.AssetClassResponse;
import com.example.backend.AssetManagamentService.dto.assetclass.CreateAssetClassRequest;
import com.example.backend.AssetManagamentService.entity.AssetClass;
import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.DuplicateResourceException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.AssetManagamentService.mapper.AssetManagementMapper;
import com.example.backend.AssetManagamentService.repository.AssetClassRepository;
import com.example.backend.AssetManagamentService.service.AssetClassService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AssetClassServiceImpl implements AssetClassService {

    private final AssetClassRepository assetClassRepository;
    private final AssetManagementMapper mapper;

    public AssetClassServiceImpl(
            AssetClassRepository assetClassRepository,
            AssetManagementMapper mapper
    ) {
        this.assetClassRepository = assetClassRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public AssetClassResponse createAssetClass(
            CreateAssetClassRequest request
    ) {
        validateRequest(request);

        String normalizedCode =
                request.getCode().trim().toUpperCase();

        if (assetClassRepository.existsByCode(normalizedCode)) {
            throw new DuplicateResourceException(
                    "Asset class with code "
                            + normalizedCode
                            + " already exists"
            );
        }

        AssetClass assetClass = new AssetClass();
        assetClass.setCode(normalizedCode);
        assetClass.setDescription(
                request.getDescription().trim()
        );

        AssetClass saved =
                assetClassRepository.save(assetClass);

        return mapper.toAssetClassResponse(saved);
    }

    @Override
    public AssetClassResponse getAssetClass(Long assetClassId) {
        AssetClass assetClass = assetClassRepository
                .findById(assetClassId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Asset class not found with id "
                                        + assetClassId
                        )
                );

        return mapper.toAssetClassResponse(assetClass);
    }

    @Override
    public List<AssetClassResponse> getAllAssetClasses() {
        return assetClassRepository.findAll()
                .stream()
                .map(mapper::toAssetClassResponse)
                .toList();
    }

    private void validateRequest(CreateAssetClassRequest request) {
        if (request == null) {
            throw new BusinessValidationException(
                    "Asset-class request is required"
            );
        }

        if (request.getCode() == null
                || request.getCode().isBlank()) {
            throw new BusinessValidationException(
                    "Asset-class code is required"
            );
        }

        if (request.getDescription() == null
                || request.getDescription().isBlank()) {
            throw new BusinessValidationException(
                    "Asset-class description is required"
            );
        }
    }
}