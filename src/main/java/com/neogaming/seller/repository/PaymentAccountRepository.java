package com.neogaming.seller.repository;

import com.neogaming.seller.domain.PaymentAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad PaymentAccount (cuentas bancarias del vendedor).
 */
public interface PaymentAccountRepository extends JpaRepository<PaymentAccount, UUID> {

    /**
     * Lista todas las cuentas bancarias de un vendedor.
     *
     * @param sellerId UUID del vendedor
     * @return Lista de cuentas ordenadas por fecha de creación
     */
    List<PaymentAccount> findBySellerIdOrderByCreatedAtDesc(UUID sellerId);

    /**
     * Busca una cuenta bancaria verificando que pertenezca al vendedor.
     * Centraliza la validación de propiedad en una sola consulta.
     *
     * @param id       UUID de la cuenta
     * @param sellerId UUID del vendedor propietario
     * @return La cuenta si existe y pertenece al vendedor
     */
    Optional<PaymentAccount> findByIdAndSellerId(UUID id, UUID sellerId);

    /**
     * Obtiene la cuenta activa actual del vendedor.
     * Solo puede haber una cuenta activa a la vez.
     *
     * @param sellerId UUID del vendedor
     * @return La cuenta activa si existe
     */
    Optional<PaymentAccount> findBySellerIdAndActiveTrue(UUID sellerId);

    /**
     * Desactiva todas las cuentas de un vendedor.
     * Se llama antes de activar una nueva cuenta para garantizar
     * que solo haya una cuenta activa en todo momento.
     *
     * @param sellerId UUID del vendedor
     */
    @Modifying
    @Query("UPDATE PaymentAccount pa SET pa.active = false WHERE pa.sellerId = :sellerId")
    void deactivateAllBySellerId(UUID sellerId);
}
