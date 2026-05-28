package com.neogaming.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Corre las migraciones Flyway como ApplicationRunner para garantizar que se ejecutan
 * antes de que el servidor acepte requests, independientemente del orden de inicialización
 * de beans JPA en Spring Boot 4.x.
 *
 * spring.flyway.enabled=false desactiva la autoconfiguración de Spring Boot para evitar
 * conflictos con este runner.
 */
@Component
@Order(1)
public class FlywayMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FlywayMigrationRunner.class);

    private final DataSource dataSource;

    public FlywayMigrationRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Ejecutando migraciones Flyway...");
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
        flyway.repair();
        MigrateResult result = flyway.migrate();
        log.info("Flyway completado: {} migraciones aplicadas", result.migrationsExecuted);
    }
}
