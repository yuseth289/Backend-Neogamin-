package com.neogaming.checkout.scheduler;

import com.neogaming.checkout.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job programado que procesa checkouts expirados.
 *
 * Se ejecuta cada 5 minutos y libera el stock reservado de los
 * checkouts que superaron su tiempo de expiración sin completar el pago.
 *
 * Esto evita que el stock quede bloqueado indefinidamente por usuarios
 * que abandonaron el proceso de pago.
 *
 * Requiere @EnableScheduling en la clase principal NeoGamingApplication.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CheckoutExpirationJob {

    private final CheckoutService checkoutService;

    /**
     * Busca y procesa los checkouts expirados cada 5 minutos.
     * La expresión cron "0 *\/5 * * * *" se ejecuta cada 5 minutos exactos.
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void procesarExpirados() {
        log.info("Iniciando limpieza de checkouts expirados...");
        checkoutService.procesarCheckoutsExpirados();
        log.info("Limpieza de checkouts expirados completada.");
    }
}
