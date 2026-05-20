package com.neogaming.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuración del RestTemplate para llamadas HTTP externas.
 *
 * Usado por MercadoPagoClient para comunicarse con la API de Mercado Pago.
 * Se define como bean para permitir su inyección con @RequiredArgsConstructor
 * y facilitar el mock en pruebas.
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Bean de RestTemplate con configuración por defecto.
     * En producción se puede configurar con timeouts y retry logic.
     *
     * @return Instancia de RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
