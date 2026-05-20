package com.neogaming.seller.dto.request;

import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para que el vendedor actualice su perfil público.
 *
 * Solo se pueden modificar los datos de la tienda (nombre, descripción, etc.)
 * y los de contacto. Los datos fiscales (NIT, régimen) no se pueden cambiar
 * sin pasar por un proceso de revisión del administrador.
 */
public record UpdateSellerRequest(

        @Size(max = 100, message = "El nombre de la tienda no puede superar los 100 caracteres")
        String storeName,

        @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres")
        String storeDescription,

        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        String phone,

        @Size(max = 300, message = "La dirección no puede superar los 300 caracteres")
        String address,

        @Size(max = 100, message = "La ciudad no puede superar los 100 caracteres")
        String city,

        @Size(max = 100, message = "El departamento no puede superar los 100 caracteres")
        String department
) {}
