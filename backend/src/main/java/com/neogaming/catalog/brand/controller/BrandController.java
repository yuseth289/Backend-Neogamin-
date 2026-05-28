package com.neogaming.catalog.brand.controller;

import com.neogaming.catalog.brand.dto.BrandRequest;
import com.neogaming.catalog.brand.dto.BrandResponse;
import com.neogaming.catalog.brand.service.BrandService;
import com.neogaming.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints públicos de marcas (GET) y de administrador (POST/PUT/DELETE).
 *
 * GET  /brands           → Lista de marcas activas (sin autenticación)
 * GET  /brands/all       → Todas las marcas incluyendo inactivas (admin)
 * POST /brands           → Crear marca (admin)
 * PUT  /brands/{id}      → Actualizar marca (admin)
 * DELETE /brands/{id}    → Desactivar marca (admin)
 * PUT  /brands/{id}/activate → Reactivar marca (admin)
 */
@RestController
@RequestMapping("/brands")
@RequiredArgsConstructor
@Tag(name = "Marcas", description = "Gestión de marcas del catálogo")
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    @Operation(summary = "Listar marcas activas", description = "Retorna las marcas activas ordenadas por displayOrder. No requiere autenticación.")
    public ResponseEntity<ApiResponse<List<BrandResponse>>> listarActivas() {
        return ResponseEntity.ok(ApiResponse.ok(brandService.listarActivas()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Listar todas las marcas (admin)", description = "Incluye marcas inactivas. Solo administradores.")
    public ResponseEntity<ApiResponse<List<BrandResponse>>> listarTodas() {
        return ResponseEntity.ok(ApiResponse.ok(brandService.listarTodas()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Crear marca")
    public ResponseEntity<ApiResponse<BrandResponse>> crear(@Valid @RequestBody BrandRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(brandService.crear(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Actualizar marca")
    public ResponseEntity<ApiResponse<BrandResponse>> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody BrandRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Marca actualizada", brandService.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Desactivar marca (soft delete)")
    public ResponseEntity<ApiResponse<Void>> desactivar(@PathVariable UUID id) {
        brandService.desactivar(id);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Reactivar marca")
    public ResponseEntity<ApiResponse<Void>> activar(@PathVariable UUID id) {
        brandService.activar(id);
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
