package com.neogaming.catalog.category.mapper;

import com.neogaming.catalog.category.domain.Category;
import com.neogaming.catalog.category.dto.response.CategoryResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper para convertir entidades Category a sus DTOs de respuesta.
 */
@Component
public class CategoryMapper {

    /**
     * Convierte una categoría a su DTO sin hijos (para consultas individuales).
     *
     * @param category Entidad de la categoría
     * @return DTO con lista de hijos vacía
     */
    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getImageUrl(),
                category.getParentId(),
                List.of()  // Sin hijos — se pueblan en el servicio para el árbol
        );
    }

    /**
     * Convierte una categoría padre con su lista de subcategorías ya cargadas.
     * Usado al construir el árbol completo de categorías.
     *
     * @param category Entidad de la categoría padre
     * @param children Lista de subcategorías ya convertidas a DTO
     * @return DTO del padre con sus hijos incluidos
     */
    public CategoryResponse toResponseWithChildren(Category category, List<CategoryResponse> children) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getImageUrl(),
                category.getParentId(),
                children
        );
    }
}
