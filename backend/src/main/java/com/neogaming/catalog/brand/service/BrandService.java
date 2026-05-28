package com.neogaming.catalog.brand.service;

import com.neogaming.catalog.brand.domain.Brand;
import com.neogaming.catalog.brand.dto.BrandRequest;
import com.neogaming.catalog.brand.dto.BrandResponse;
import com.neogaming.catalog.brand.repository.BrandRepository;
import com.neogaming.common.exception.BusinessRuleException;
import com.neogaming.common.exception.ResourceNotFoundException;
import com.neogaming.common.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BrandService {

    private final BrandRepository brandRepository;

    @Transactional(readOnly = true)
    public List<BrandResponse> listarActivas() {
        return brandRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BrandResponse> listarTodas() {
        return brandRepository.findAllByOrderByDisplayOrderAscNameAsc()
                .stream().map(this::toResponse).toList();
    }

    public BrandResponse crear(BrandRequest request) {
        if (brandRepository.existsByName(request.name())) {
            throw new BusinessRuleException(
                    "Ya existe una marca con el nombre '" + request.name() + "'",
                    "BRAND_NAME_DUPLICATE"
            );
        }
        String slug = generarSlugUnico(request.name());
        Brand brand = Brand.builder()
                .name(request.name())
                .slug(slug)
                .displayOrder(request.displayOrder())
                .active(true)
                .build();
        return toResponse(brandRepository.save(brand));
    }

    public BrandResponse actualizar(UUID id, BrandRequest request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marca", id.toString()));

        if (!brand.getName().equals(request.name())) {
            if (brandRepository.existsByName(request.name())) {
                throw new BusinessRuleException(
                        "Ya existe una marca con el nombre '" + request.name() + "'",
                        "BRAND_NAME_DUPLICATE"
                );
            }
            brand.setSlug(generarSlugUnico(request.name()));
            brand.setName(request.name());
        }
        brand.setDisplayOrder(request.displayOrder());
        return toResponse(brandRepository.save(brand));
    }

    public void desactivar(UUID id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marca", id.toString()));
        brand.setActive(false);
        brandRepository.save(brand);
    }

    public void activar(UUID id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marca", id.toString()));
        brand.setActive(true);
        brandRepository.save(brand);
    }

    private BrandResponse toResponse(Brand brand) {
        return new BrandResponse(
                brand.getId(),
                brand.getName(),
                brand.getSlug(),
                brand.getDisplayOrder(),
                brand.isActive()
        );
    }

    private String generarSlugUnico(String name) {
        String base = SlugUtils.toSlug(name);
        String candidate = base;
        int n = 2;
        while (brandRepository.existsBySlug(candidate)) {
            candidate = base + "-" + n++;
        }
        return candidate;
    }
}
