package com.neogaming.wishlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de creación de una nueva lista de deseos.
 */
public record CreateWishlistRequest(

        @NotBlank(message = "El nombre de la lista es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String name,

        boolean isPublic
) {}
