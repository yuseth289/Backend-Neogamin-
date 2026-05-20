package com.neogaming.common.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Envoltorio estándar para respuestas paginadas de la API NeoGaming.
 *
 * Se usa en todos los endpoints que retornan listas con paginación.
 * Ejemplo de uso en controlador:
 *   Page<ProductoResponse> page = productoService.listar(pageable);
 *   return ApiResponse.ok(PageResponse.from(page));
 *
 * Formato JSON resultante:
 * {
 *   "content":       [ { ... }, { ... } ],
 *   "page":          0,
 *   "size":          20,
 *   "totalElements": 150,
 *   "totalPages":    8,
 *   "first":         true,
 *   "last":          false
 * }
 *
 * @param <T> Tipo de los elementos en la lista paginada
 */
public record PageResponse<T>(

        /** Lista de elementos de la página actual */
        List<T> content,

        /** Número de página actual (base 0) */
        int page,

        /** Cantidad de elementos por página */
        int size,

        /** Total de elementos en todas las páginas */
        long totalElements,

        /** Total de páginas disponibles */
        int totalPages,

        /** true si es la primera página */
        boolean first,

        /** true si es la última página */
        boolean last
) {

    /**
     * Convierte un objeto Page de Spring Data en un PageResponse.
     *
     * @param page Resultado paginado de un repositorio Spring Data JPA
     * @return PageResponse con la misma información estructurada para la API
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
