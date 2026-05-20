package com.neogaming.address.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para crear o actualizar una dirección.
 * Se usa tanto en POST (crear) como en PUT (actualizar).
 *
 * Campos obligatorios: label, street, number, city, department.
 * Campos opcionales:   floor, apartment, postalCode.
 * El país se establece automáticamente como "Colombia".
 *
 * Ejemplo de request body:
 * {
 *   "label":      "Casa",
 *   "street":     "Cra 7",
 *   "number":     "# 32-15",
 *   "floor":      "3",
 *   "apartment":  "301",
 *   "city":       "Bogotá",
 *   "department": "Cundinamarca",
 *   "postalCode": "110311"
 * }
 */
public record AddressRequest(

        @NotBlank(message = "La etiqueta de la dirección es obligatoria (ej: Casa, Trabajo)")
        @Size(max = 50, message = "La etiqueta no puede superar los 50 caracteres")
        String label,

        @NotBlank(message = "La calle es obligatoria")
        @Size(max = 200, message = "La calle no puede superar los 200 caracteres")
        String street,

        @NotBlank(message = "El número es obligatorio")
        @Size(max = 20, message = "El número no puede superar los 20 caracteres")
        String number,

        @Size(max = 10, message = "El piso no puede superar los 10 caracteres")
        String floor,

        @Size(max = 10, message = "El apartamento no puede superar los 10 caracteres")
        String apartment,

        @NotBlank(message = "La ciudad es obligatoria")
        @Size(max = 100, message = "La ciudad no puede superar los 100 caracteres")
        String city,

        @NotBlank(message = "El departamento es obligatorio")
        @Size(max = 100, message = "El departamento no puede superar los 100 caracteres")
        String department,

        @Size(max = 20, message = "El código postal no puede superar los 20 caracteres")
        String postalCode
) {}
