package com.neogaming.catalog.category.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * DTO de salida con los datos de una categoría.
 *
 * Cuando se lista el árbol de categorías, las subcategorías se incluyen
 * en el campo "children" de su categoría padre.
 * Para subcategorías individuales, children = [].
 */
public record CategoryResponse(

        UUID id,

        /** Nombre visible de la categoría */
        String name,

        /** Slug URL-amigable para filtros del catálogo */
        String slug,

        String description,
        String imageUrl,

        /** Nombre del ícono Lucide asociado (ej: lucideGamepad2). Nulo = ícono por defecto. */
        String iconName,

        /** UUID del padre. null si es categoría raíz. */
        UUID parentId,

        /**
         * Subcategorías hijas (solo poblado al listar el árbol completo).
         * Vacío cuando se consulta una categoría individual.
         */
        List<CategoryResponse> children
) {}
