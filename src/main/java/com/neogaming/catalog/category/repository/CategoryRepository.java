package com.neogaming.catalog.category.repository;

import com.neogaming.catalog.category.domain.Category;
import com.neogaming.common.enums.EstadoGenerico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad Category.
 */
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /**
     * Lista todas las categorías raíz (sin padre) con un estado dado.
     * Usado para construir el menú de navegación del catálogo.
     *
     * @param status Estado a filtrar
     * @return Categorías padre activas
     */
    List<Category> findByParentIdIsNullAndStatusOrderByNameAsc(EstadoGenerico status);

    /**
     * Lista todas las subcategorías de un padre con un estado dado.
     * Usado para mostrar las subcategorías al seleccionar una categoría padre.
     *
     * @param parentId UUID de la categoría padre
     * @param status   Estado a filtrar
     * @return Subcategorías activas del padre
     */
    List<Category> findByParentIdAndStatusOrderByNameAsc(UUID parentId, EstadoGenerico status);

    /**
     * Busca una categoría por su slug.
     * Usado en filtros de búsqueda del catálogo: GET /products?category={slug}
     *
     * @param slug Slug de la categoría
     * @return La categoría si existe
     */
    Optional<Category> findBySlug(String slug);

    /**
     * Verifica si ya existe una categoría con el slug dado.
     * Previene duplicados al crear o actualizar categorías.
     *
     * @param slug Slug a verificar
     * @return true si el slug ya está en uso
     */
    boolean existsBySlug(String slug);
}
