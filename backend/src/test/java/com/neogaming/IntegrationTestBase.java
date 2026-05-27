package com.neogaming;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Clase base para todos los tests de integración de NeoGaming.
 *
 * Levanta un contenedor PostgreSQL 17 compartido entre todos los tests
 * (anotado como static para reutilizarlo — evita el costo de arrancar
 * un contenedor por clase). Spring inyecta la URL de conexión vía
 * DynamicPropertySource antes de inicializar el contexto.
 *
 * Flyway corre automáticamente al arrancar el contexto, aplicando todas
 * las migraciones sobre el PostgreSQL del contenedor.
 *
 * Perfil activo: "integration" — usa application-integration.yml
 * que habilita Flyway y deshabilita el servidor de correo real.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@Testcontainers
public abstract class IntegrationTestBase {

    @LocalServerPort
    protected int port;

    protected final RestTemplate restTemplate = new RestTemplate() {{
        setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) { return false; }
        });
    }};

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("neogaming_test")
            .withUsername("neogaming")
            .withPassword("neogaming");

    @DynamicPropertySource
    static void configurarPropiedades(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
