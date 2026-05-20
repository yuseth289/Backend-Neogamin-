package com.neogaming.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de la documentación OpenAPI (Swagger) para NeoGaming.
 *
 * Define la información general de la API y el esquema de autenticación JWT.
 *
 * Acceso a la documentación (entorno local):
 *  - Swagger UI:  http://localhost:8080/swagger-ui.html
 *  - JSON spec:   http://localhost:8080/v3/api-docs
 *
 * Para probar endpoints protegidos en Swagger:
 *  1. Hacer login en POST /auth/login
 *  2. Copiar el accessToken de la respuesta
 *  3. Hacer clic en "Authorize" y pegar: Bearer <accessToken>
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "NeoGaming API",
                version = "1.0.0",
                description = "API REST del marketplace gaming y electrónicos NeoGaming — Colombia",
                contact = @Contact(
                        name = "NeoGaming Dev Team",
                        email = "dev@neogaming.co"
                )
        )
)
@SecurityScheme(
        name = "bearerAuth",            // Nombre del esquema referenciado en cada endpoint
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",           // Solo informativo para la UI de Swagger
        description = "Token JWT obtenido en POST /auth/login. Formato: Bearer <token>"
)
public class SwaggerConfig {
    // La configuración se realiza completamente a través de anotaciones.
    // springdoc-openapi escanea automáticamente todos los @RestController
    // y genera la especificación OpenAPI sin necesidad de beans adicionales.
}
