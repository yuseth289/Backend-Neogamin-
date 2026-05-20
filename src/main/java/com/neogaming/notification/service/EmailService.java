package com.neogaming.notification.service;

import com.neogaming.notification.dto.EmailContext;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

/**
 * Servicio de envío de correos electrónicos con plantillas Thymeleaf.
 *
 * Todos los métodos son asíncronos (@Async) — los fallos de correo no
 * interrumpen el flujo principal de negocio (pago, registro, etc.).
 *
 * Las plantillas HTML se encuentran en src/main/resources/templates/email/.
 * El idioma de renderizado es español (es_CO).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    /**
     * Envía un correo HTML renderizado con Thymeleaf de forma asíncrona.
     * Si el envío falla, solo se registra el error en el log (no se relanza).
     *
     * @param ctx Contexto con destinatario, asunto, plantilla y variables
     */
    @Async
    public void enviar(EmailContext ctx) {
        try {
            Context thymeleafCtx = new Context(Locale.forLanguageTag("es-CO"));
            thymeleafCtx.setVariables(ctx.variables());

            String html = templateEngine.process("email/" + ctx.templateName(), thymeleafCtx);

            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setTo(ctx.to());
            helper.setSubject(ctx.subject());
            helper.setText(html, true);
            helper.setFrom("noreply@neogaming.co");

            mailSender.send(mensaje);
            log.info("Correo '{}' enviado a {}", ctx.templateName(), ctx.to());

        } catch (MessagingException | MailException e) {
            log.error("Error al enviar correo '{}' a {}: {}", ctx.templateName(), ctx.to(), e.getMessage());
        }
    }
}
