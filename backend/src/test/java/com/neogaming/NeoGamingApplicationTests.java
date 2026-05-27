package com.neogaming;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Prueba de carga del contexto de Spring Boot.
 *
 * Verifica que toda la configuración de beans, JPA, seguridad y módulos
 * se inicializa correctamente sin errores al arrancar la aplicación.
 *
 * Usa el perfil "test" (H2 + Flyway desactivado) para una verificación
 * rápida de la configuración sin necesidad de PostgreSQL ni Testcontainers.
 */
@SpringBootTest
@ActiveProfiles("test")
class NeoGamingApplicationTests {

    /**
     * Prueba que el contexto de Spring se carga sin errores.
     * Si algún bean tiene dependencias rotas o configuración inválida,
     * esta prueba fallará con una excepción descriptiva.
     */
    @Test
    void contextLoads() {
        // Si llegamos aquí, el contexto cargó correctamente
    }
}
