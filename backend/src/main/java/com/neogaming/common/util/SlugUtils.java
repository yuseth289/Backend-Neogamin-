package com.neogaming.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utilidad para generar slugs URL-friendly a partir de texto.
 *
 * Un slug es una versión del texto que puede usarse en URLs:
 *  - Sin caracteres especiales ni tildes
 *  - Solo letras minúsculas, números y guiones
 *  - Espacios reemplazados por guiones
 *
 * Ejemplos:
 *  "Teclado Mecánico RGB"    →  "teclado-mecanico-rgb"
 *  "Monitor Curvo 27\" 4K"   →  "monitor-curvo-27-4k"
 *  "Audífonos Gamer Pro #1"  →  "audifonos-gamer-pro-1"
 *
 * Se usa para generar los slugs de productos, categorías y tiendas de vendedores.
 */
public final class SlugUtils {

    /** Patrón para eliminar cualquier carácter que no sea letra minúscula, número o guion */
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9-]");

    /** Patrón para colapsar múltiples guiones consecutivos en uno solo */
    private static final Pattern MULTIPLE_HYPHENS = Pattern.compile("-{2,}");

    /** Constructor privado: clase de utilidad, no se instancia */
    private SlugUtils() {}

    /**
     * Convierte un texto cualquiera en un slug URL-friendly.
     *
     * Proceso:
     * 1. Normaliza tildes y caracteres diacríticos (ej: á → a)
     * 2. Convierte a minúsculas
     * 3. Reemplaza espacios por guiones
     * 4. Elimina caracteres no alfanuméricos
     * 5. Colapsa guiones múltiples
     *
     * @param input Texto original (ej: nombre de producto o tienda)
     * @return Slug URL-friendly
     */
    public static String toSlug(String input) {
        // Eliminar acentos y diacríticos (á → a, ñ → n, etc.)
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // Convertir a minúsculas y recortar espacios extremos
        String lower = normalized.toLowerCase(Locale.ROOT).trim();

        // Reemplazar espacios por guiones
        String withHyphens = lower.replace(" ", "-");

        // Eliminar todo lo que no sea letra, número o guion
        String clean = NON_ALPHANUMERIC.matcher(withHyphens).replaceAll("");

        // Colapsar "--" o "---" en un solo "-"
        return MULTIPLE_HYPHENS.matcher(clean).replaceAll("-");
    }
}
