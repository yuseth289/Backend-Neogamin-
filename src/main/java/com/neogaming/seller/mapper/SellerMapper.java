package com.neogaming.seller.mapper;

import com.neogaming.seller.domain.PaymentAccount;
import com.neogaming.seller.domain.Seller;
import com.neogaming.seller.dto.response.PaymentAccountResponse;
import com.neogaming.seller.dto.response.PublicSellerResponse;
import com.neogaming.seller.dto.response.SellerResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper responsable de convertir entidades Seller y PaymentAccount
 * a sus respectivos DTOs de respuesta.
 *
 * Implementación manual para control explícito sobre qué campos
 * se exponen. El token de Mercado Pago y el número de cuenta completo
 * nunca se incluyen en las respuestas.
 */
@Component
public class SellerMapper {

    /**
     * Convierte un Seller en un SellerResponse completo.
     * Usado para el propio vendedor y para administradores.
     *
     * @param seller Entidad del vendedor
     * @return DTO con todos los datos del perfil (sin datos sensibles como mpAccessToken)
     */
    public SellerResponse toResponse(Seller seller) {
        return new SellerResponse(
                seller.getId(),
                seller.getUserId(),
                seller.getStoreName(),
                seller.getStoreSlug(),
                seller.getStoreDescription(),
                seller.getStoreLogoUrl(),
                seller.getStoreBannerUrl(),
                seller.getTipoDocumento(),
                seller.getNumeroDocumento(),
                seller.getRazonSocial(),
                seller.getTipoRegimen(),
                seller.getPhone(),
                seller.getAddress(),
                seller.getCity(),
                seller.getDepartment(),
                seller.getMpUserId(),  // Solo el ID de MP, nunca el access token
                seller.getStatus(),
                seller.getCreatedAt(),
                seller.getUpdatedAt()
        );
    }

    /**
     * Convierte un Seller en un PublicSellerResponse para compradores.
     * Solo incluye los datos visibles públicamente de la tienda.
     *
     * @param seller Entidad del vendedor
     * @return DTO con solo los datos públicos de la tienda
     */
    public PublicSellerResponse toPublicResponse(Seller seller) {
        return new PublicSellerResponse(
                seller.getId(),
                seller.getStoreName(),
                seller.getStoreSlug(),
                seller.getStoreDescription(),
                seller.getStoreLogoUrl(),
                seller.getStoreBannerUrl(),
                seller.getCity(),
                seller.getDepartment()
        );
    }

    /**
     * Convierte una PaymentAccount en su DTO de respuesta.
     * El número de cuenta se enmascara mostrando solo los últimos 4 dígitos.
     *
     * @param account Entidad de la cuenta bancaria
     * @return DTO con el número de cuenta enmascarado
     */
    public PaymentAccountResponse toPaymentAccountResponse(PaymentAccount account) {
        return new PaymentAccountResponse(
                account.getId(),
                account.getBankName(),
                account.getAccountType(),
                maskAccountNumber(account.getAccountNumber()),
                account.getAccountHolder(),
                account.getDocumentType(),
                account.getDocumentNumber(),
                account.isActive(),
                account.getCreatedAt()
        );
    }

    /**
     * Enmascara un número de cuenta bancaria para no exponer datos sensibles.
     * Muestra solo los últimos 4 dígitos precedidos de asteriscos.
     * Ejemplo: "12345678901" → "****8901"
     *
     * @param accountNumber Número de cuenta completo
     * @return Número enmascarado o "****" si tiene menos de 4 dígitos
     */
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        // Tomar los últimos 4 caracteres y anteponer asteriscos
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
