package com.neogaming.auth;

import com.neogaming.IntegrationTestBase;
import com.neogaming.auth.dto.request.LoginRequest;
import com.neogaming.auth.dto.request.RegisterRequest;
import com.neogaming.auth.repository.SessionRepository;
import com.neogaming.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de integración para el flujo de autenticación.
 *
 * Cubre:
 *  - Registro de usuario nuevo → 201 con tokens
 *  - Email duplicado → 409
 *  - Login correcto → tokens válidos
 *  - Login con contraseña incorrecta → 401
 *  - Refresh token válido → nuevo access token
 *  - Logout invalida el refresh token
 */
@DisplayName("Auth — Integración")
class AuthIntegrationTest extends IntegrationTestBase {

    @Autowired private UserRepository userRepository;
    @Autowired private SessionRepository sessionRepository;

    @BeforeEach
    void limpiar() {
        sessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Registro exitoso devuelve 201 con tokens")
    void registroExitoso() {
        RegisterRequest req = new RegisterRequest(
                "test@neogaming.co", "Password123!", "Carlos", "García", "3001234567");

        ResponseEntity<Map<String, Object>> response = post("/auth/register", req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> data = extraerData(response);
        assertThat(data).containsKeys("accessToken", "refreshToken", "userId", "role");
        assertThat(data.get("role")).isEqualTo("CLIENT");
    }

    @Test
    @DisplayName("Registro con email duplicado devuelve 409")
    void registroDuplicadoDevuelve409() {
        RegisterRequest req = new RegisterRequest(
                "dup@neogaming.co", "Password123!", "Ana", "López", null);

        post("/auth/register", req);
        ResponseEntity<Map<String, Object>> segunda = post("/auth/register", req);

        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Login correcto devuelve access y refresh token")
    void loginCorrecto() {
        post("/auth/register", new RegisterRequest(
                "login@neogaming.co", "Password123!", "Luis", "Torres", null));

        ResponseEntity<Map<String, Object>> response =
                post("/auth/login", new LoginRequest("login@neogaming.co", "Password123!"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = extraerData(response);
        assertThat((String) data.get("accessToken")).isNotBlank();
        assertThat((String) data.get("refreshToken")).isNotBlank();
    }

    @Test
    @DisplayName("Login con contraseña incorrecta devuelve 401")
    void loginContraseniaIncorrecta() {
        post("/auth/register", new RegisterRequest(
                "wrong@neogaming.co", "Password123!", "María", "Pérez", null));

        ResponseEntity<Map<String, Object>> response =
                post("/auth/login", new LoginRequest("wrong@neogaming.co", "ContraseniaErronea!"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Refresh token válido devuelve nuevo access token")
    void refreshTokenValido() {
        ResponseEntity<Map<String, Object>> regResp = post("/auth/register",
                new RegisterRequest("refresh@neogaming.co", "Password123!", "Pedro", "Ruiz", null));

        String refreshToken = (String) extraerData(regResp).get("refreshToken");

        ResponseEntity<Map<String, Object>> refreshResp =
                post("/auth/refresh", Map.of("refreshToken", refreshToken));

        assertThat(refreshResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((String) extraerData(refreshResp).get("accessToken")).isNotBlank();
    }

    @Test
    @DisplayName("Logout invalida el refresh token — segundo uso devuelve 401")
    void logoutInvalidaRefreshToken() {
        ResponseEntity<Map<String, Object>> regResp = post("/auth/register",
                new RegisterRequest("logout@neogaming.co", "Password123!", "Sofía", "Hernández", null));

        Map<String, Object> data = extraerData(regResp);
        String accessToken  = (String) data.get("accessToken");
        String refreshToken = (String) data.get("refreshToken");

        // Logout con el access token en el header
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.exchange(url("/auth/logout"), HttpMethod.POST,
                new HttpEntity<>(headers), mapClass());

        // El refresh token ya no debe funcionar
        ResponseEntity<Map<String, Object>> refreshResp =
                post("/auth/refresh", Map.of("refreshToken", refreshToken));

        assertThat(refreshResp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private <B> ResponseEntity<Map<String, Object>> post(String path, B body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(url(path), HttpMethod.POST,
                new HttpEntity<>(body, headers), mapClass());
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @SuppressWarnings("unchecked")
    private static Class<Map<String, Object>> mapClass() {
        return (Class<Map<String, Object>>) (Class<?>) Map.class;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extraerData(ResponseEntity<Map<String, Object>> resp) {
        assertThat(resp.getBody()).isNotNull();
        return (Map<String, Object>) resp.getBody().get("data");
    }
}
