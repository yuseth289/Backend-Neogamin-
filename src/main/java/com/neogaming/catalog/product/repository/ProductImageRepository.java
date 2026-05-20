package com.neogaming.catalog.product.repository;

import com.neogaming.catalog.product.domain.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad ProductImage.
 */
public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    /**
     * Lista todas las imágenes de un producto ordenadas para la galería.
     * La imagen principal (isPrimary = true) aparece primera, luego por sortOrder.
     *
     * @param productId UUID del producto
     * @return Lista de imágenes ordenadas
     */
    List<ProductImage> findByProductIdOrderByPrimaryDescSortOrderAsc(UUID productId);

    /**
     * Busca la imagen principal actual de un producto.
     * Usado antes de cambiar la imagen principal para desmarcar la actual.
     *
     * @param productId UUID del producto
     * @return La imagen principal si existe
     */
    Optional<ProductImage> findByProductIdAndPrimaryTrue(UUID productId);

    /**
     * Busca una imagen verificando que pertenezca al producto.
     * Centraliza la validación de propiedad para operaciones del vendedor.
     *
     * @param id        UUID de la imagen
     * @param productId UUID del producto
     * @return La imagen si existe y pertenece al producto
     */
    Optional<ProductImage> findByIdAndProductId(UUID id, UUID productId);

    /**
     * Desmarca todas las imágenes de un producto como no-principales.
     * Se llama antes de establecer una nueva imagen principal para garantizar
     * que solo haya una imagen principal por producto en todo momento.
     *
     * @param productId UUID del producto
     */
    @Modifying
    @Query("UPDATE ProductImage pi SET pi.primary = false WHERE pi.productId = :productId")
    void clearPrimaryByProductId(UUID productId);

    /**
     * Cuenta cuántas imágenes tiene un producto.
     * Permite validar el límite máximo de imágenes por producto.
     *
     * @param productId UUID del producto
     * @return Número de imágenes del producto
     */
    long countByProductId(UUID productId);
}
