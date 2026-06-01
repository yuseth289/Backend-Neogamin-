package com.neogaming.catalog.category.service;

import com.neogaming.catalog.category.domain.Category;
import com.neogaming.catalog.category.dto.request.CategoryRequest;
import com.neogaming.catalog.category.dto.response.CategoryResponse;
import com.neogaming.catalog.category.mapper.CategoryMapper;
import com.neogaming.catalog.category.repository.CategoryRepository;
import com.neogaming.common.enums.EstadoGenerico;
import com.neogaming.common.exception.BusinessRuleException;
import com.neogaming.common.exception.ResourceNotFoundException;
import com.neogaming.common.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de gestión de categorías para NeoGaming.
 *
 * Reglas de negocio implementadas:
 *  - Solo administradores pueden crear, editar y desactivar categorías
 *  - Las categorías se organizan en dos niveles: padre → subcategoría
 *  - No se permite crear subcategorías de subcategorías (máximo 2 niveles)
 *  - Los slugs son únicos y se generan automáticamente desde el nombre
 *  - Las categorías inactivas no aparecen en el catálogo público
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    /**
     * Retorna el árbol de categorías activas para el catálogo público.
     *
     * Estructura de la respuesta: lista de categorías padre, cada una
     * con su lista de subcategorías en el campo "children".
     *
     * @return Lista de categorías raíz con sus subcategorías anidadas
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> obtenerArbol() {
        // Obtener todas las categorías padre (sin parentId)
        List<Category> padres = categoryRepository
                .findByParentIdIsNullAndStatusOrderByNameAsc(EstadoGenerico.ACTIVE);

        // Para cada padre, obtener sus hijos y armar la respuesta anidada
        return padres.stream()
                .map(padre -> {
                    List<CategoryResponse> hijos = categoryRepository
                            .findByParentIdAndStatusOrderByNameAsc(padre.getId(), EstadoGenerico.ACTIVE)
                            .stream()
                            .map(categoryMapper::toResponse)
                            .toList();
                    return categoryMapper.toResponseWithChildren(padre, hijos);
                })
                .toList();
    }

    /**
     * Obtiene los detalles de una categoría por su slug.
     * Incluye sus subcategorías si es una categoría padre.
     *
     * @param slug Slug de la categoría
     * @return Datos de la categoría con sus hijos si los tiene
     * @throws ResourceNotFoundException si no existe o está inactiva
     */
    @Transactional(readOnly = true)
    public CategoryResponse obtenerPorSlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .filter(c -> c.getStatus() == EstadoGenerico.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", slug));

        // Si tiene subcategorías, incluirlas en la respuesta
        List<CategoryResponse> hijos = categoryRepository
                .findByParentIdAndStatusOrderByNameAsc(category.getId(), EstadoGenerico.ACTIVE)
                .stream()
                .map(categoryMapper::toResponse)
                .toList();

        return categoryMapper.toResponseWithChildren(category, hijos);
    }

    /**
     * Crea una nueva categoría (operación exclusiva de administrador).
     *
     * Si se especifica un parentId, valida que el padre existe y que
     * no es ya una subcategoría (para no superar los 2 niveles).
     *
     * @param request Datos de la nueva categoría
     * @return La categoría recién creada
     * @throws ResourceNotFoundException si el padre especificado no existe
     * @throws BusinessRuleException     si se intenta crear un tercer nivel de jerarquía
     */
    public CategoryResponse crear(CategoryRequest request) {
        // Si tiene padre, validar que el padre existe y no tiene padre propio
        if (request.parentId() != null) {
            Category padre = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Categoría padre", request.parentId().toString()
                    ));
            // No permitir más de 2 niveles de jerarquía
            if (padre.getParentId() != null) {
                throw new BusinessRuleException(
                        "No se pueden crear subcategorías de subcategorías. Máximo 2 niveles.",
                        "JERARQUIA_MAXIMA_EXCEDIDA"
                );
            }
        }

        String slug = generarSlugUnico(request.name());

        Category category = Category.builder()
                .name(request.name())
                .slug(slug)
                .description(request.description())
                .imageUrl(request.imageUrl())
                .iconName(request.iconName())
                .parentId(request.parentId())
                .status(EstadoGenerico.ACTIVE)
                .build();

        List<CategoryResponse> hijos = List.of();
        return categoryMapper.toResponseWithChildren(categoryRepository.save(category), hijos);
    }

    /**
     * Actualiza los datos de una categoría existente (operación de administrador).
     *
     * No se puede cambiar el padre de una categoría después de creada
     * para evitar inconsistencias en el árbol de jerarquía.
     *
     * @param id      UUID de la categoría a actualizar
     * @param request Nuevos datos
     * @return La categoría actualizada
     */
    public CategoryResponse actualizar(UUID id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", id.toString()));

        // Regenerar slug si el nombre cambia
        if (!category.getName().equals(request.name())) {
            category.setSlug(generarSlugUnico(request.name()));
            category.setName(request.name());
        }
        if (request.description() != null) {
            category.setDescription(request.description());
        }
        if (request.imageUrl() != null) {
            category.setImageUrl(request.imageUrl());
        }
        category.setIconName(request.iconName());

        List<CategoryResponse> hijos = categoryRepository
                .findByParentIdAndStatusOrderByNameAsc(id, EstadoGenerico.ACTIVE)
                .stream()
                .map(categoryMapper::toResponse)
                .toList();

        return categoryMapper.toResponseWithChildren(categoryRepository.save(category), hijos);
    }

    /**
     * Desactiva una categoría (soft delete — no se elimina físicamente).
     *
     * Las subcategorías hijas también se desactivan para mantener consistencia.
     * Los productos ya en esta categoría siguen existiendo pero quedan
     * sin categoría activa hasta que el vendedor los recategorice.
     *
     * @param id UUID de la categoría a desactivar
     */
    public void desactivar(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", id.toString()));

        category.setStatus(EstadoGenerico.INACTIVE);
        categoryRepository.save(category);

        // Desactivar subcategorías hijas en cascada
        List<Category> hijas = categoryRepository
                .findByParentIdAndStatusOrderByNameAsc(id, EstadoGenerico.ACTIVE);
        hijas.forEach(hija -> {
            hija.setStatus(EstadoGenerico.INACTIVE);
            categoryRepository.save(hija);
        });
    }

    /**
     * Genera un slug único a partir del nombre de la categoría.
     * Si el slug base ya existe, agrega sufijo numérico incremental.
     *
     * @param name Nombre de la categoría
     * @return Slug único garantizado
     */
    private String generarSlugUnico(String name) {
        String baseSlug = SlugUtils.toSlug(name);
        String candidato = baseSlug;
        int contador = 2;
        while (categoryRepository.existsBySlug(candidato)) {
            candidato = baseSlug + "-" + contador;
            contador++;
        }
        return candidato;
    }
}
