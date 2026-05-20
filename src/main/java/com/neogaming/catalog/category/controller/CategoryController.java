package com.neogaming.catalog.category.controller;

import com.neogaming.catalog.category.dto.request.CategoryRequest;
import com.neogaming.catalog.category.dto.response.CategoryResponse;
import com.neogaming.catalog.category.service.CategoryService;
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
 * Controlador REST para la gestión de categorías del catálogo.
 *
 * Endpoints públicos (sin autenticación):
 *  GET  /categories           → Árbol completo de categorías activas
 *  GET  /categories/{slug}    → Detalle de una categoría con sus subcategorías
 *
 * Endpoints de administrador (requieren rol ADMIN):
 *  POST   /categories         → Crear categoría
 *  PUT    /categories/{id}    → Actualizar categoría
 *  DELETE /categories/{id}    → Desactivar categoría (soft delete)
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Categorías", description = "Gestión del árbol de categorías del catálogo")
public class CategoryController {

    private final CategoryService categoryService;

    // ===== ENDPOINTS PÚBLICOS =====

    /**
     * Retorna el árbol completo de categorías activas.
     * Cada categoría padre incluye sus subcategorías en el campo "children".
     * No requiere autenticación.
     */
    @GetMapping
    @Operation(
            summary = "Listar árbol de categorías",
            description = "Retorna todas las categorías activas organizadas en árbol (padre + subcategorías)."
    )
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> listarArbol() {
        List<CategoryResponse> arbol = categoryService.obtenerArbol();
        return ResponseEntity.ok(ApiResponse.ok(arbol));
    }

    /**
     * Retorna el detalle de una categoría con sus subcategorías.
     * No requiere autenticación.
     */
    @GetMapping("/{slug}")
    @Operation(
            summary = "Ver categoría por slug",
            description = "Retorna una categoría activa con sus subcategorías incluidas."
    )
    public ResponseEntity<ApiResponse<CategoryResponse>> obtenerPorSlug(@PathVariable String slug) {
        CategoryResponse response = categoryService.obtenerPorSlug(slug);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ===== ENDPOINTS DE ADMINISTRADOR =====

    /**
     * Crea una nueva categoría o subcategoría.
     * Solo administradores.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Crear categoría",
            description = "Crea una categoría o subcategoría. Máximo 2 niveles de jerarquía."
    )
    public ResponseEntity<ApiResponse<CategoryResponse>> crear(
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.crear(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    /**
     * Actualiza el nombre, descripción o imagen de una categoría.
     * Solo administradores.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Actualizar categoría",
            description = "Actualiza nombre, descripción e imagen de una categoría existente."
    )
    public ResponseEntity<ApiResponse<CategoryResponse>> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.actualizar(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Categoría actualizada correctamente", response));
    }

    /**
     * Desactiva una categoría y sus subcategorías en cascada.
     * Solo administradores.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Desactivar categoría",
            description = "Desactiva la categoría y sus subcategorías. Los productos existentes quedan sin categoría activa."
    )
    public ResponseEntity<ApiResponse<Void>> desactivar(@PathVariable UUID id) {
        categoryService.desactivar(id);
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
