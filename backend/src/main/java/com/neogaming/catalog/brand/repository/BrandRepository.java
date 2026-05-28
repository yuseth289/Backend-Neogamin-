package com.neogaming.catalog.brand.repository;

import com.neogaming.catalog.brand.domain.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {

    List<Brand> findByActiveTrueOrderByDisplayOrderAscNameAsc();

    List<Brand> findAllByOrderByDisplayOrderAscNameAsc();

    boolean existsBySlug(String slug);

    boolean existsByName(String name);
}
