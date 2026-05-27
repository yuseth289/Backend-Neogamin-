package com.neogaming.auth.service;

import com.neogaming.auth.domain.Session;
import com.neogaming.auth.dto.request.LoginRequest;
import com.neogaming.auth.dto.request.RefreshTokenRequest;
import com.neogaming.auth.dto.request.RegisterRequest;
import com.neogaming.auth.dto.response.TokenResponse;
import com.neogaming.auth.provider.JwtTokenProvider;
import com.neogaming.auth.repository.SessionRepository;
import com.neogaming.common.enums.EstadoGenerico;
import com.neogaming.common.enums.RolUsuario;
import com.neogaming.common.exception.BusinessRuleException;
import com.neogaming.common.exception.ConflictException;
import com.neogaming.common.exception.ResourceNotFoundException;
import com.neogaming.common.exception.UnauthorizedException;
import com.neogaming.user.domain.User;
import com.neogaming.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

/**
 * Servicio de autenticación y gestión de sesiones para NeoGaming.
 *
 * Operaciones disponibles:
 *  - register : Registra un nuevo usuario y retorna sus tokens iniciales
 *  - login    : Autentica credenciales y retorna un par de tokens
 *  - logout   : Revoca la sesión activa del usuario
 *  - refresh  : Rota el refresh token y emite un nuevo par de tokens
 *
 * Seguridad del refresh token:
 *  - Se genera como UUID aleatorio (valor que el cliente guarda)
 *  - En la BD se almacena el hash SHA-256 del UUID (nunca el token en texto plano)
 *  - Al usar el refresh, se revoca la sesión anterior (rotación de token)
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;

    @Value("${google.client-id:}")
    private String googleClientId;

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * Pasos:
     *  1. Verifica que el email no esté ya registrado
     *  2. Crea el usuario con rol CLIENT y contraseña hasheada
     *  3. Crea una sesión y retorna los tokens de acceso
     *
     * @param request    Datos del nuevo usuario (email, contraseña, nombre, etc.)
     * @param deviceInfo User-Agent del cliente (para identificar el dispositivo)
     * @return Par de tokens (accessToken + refreshToken) para acceso inmediato
     * @throws ConflictException si el email ya está registrado
     */
    @Transactional
    public TokenResponse register(RegisterRequest request, String deviceInfo) {
        // Verificar que el email no esté en uso
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException(
                    "El correo electrónico ya está registrado en NeoGaming",
                    "EMAIL_YA_EXISTE"
            );
        }

        // Crear el nuevo usuario con rol CLIENT por defecto
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password())) // Hash BCrypt
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .role(RolUsuario.CLIENT)
                .status(EstadoGenerico.ACTIVE)
                .emailVerified(false)
                .build();

        userRepository.save(user);

        // Crear sesión y retornar tokens
        return crearSesionYTokens(user, deviceInfo);
    }

    /**
     * Autentica a un usuario con email y contraseña.
     *
     * Pasos:
     *  1. Busca el usuario por email
     *  2. Verifica la contraseña con BCrypt
     *  3. Verifica que la cuenta esté activa
     *  4. Crea una sesión y retorna los tokens
     *
     * Nota de seguridad: el mensaje de error es genérico tanto si el email no
     * existe como si la contraseña es incorrecta. Esto evita enumeración de usuarios.
     *
     * @param request    Email y contraseña del usuario
     * @param deviceInfo User-Agent del cliente
     * @return Par de tokens si las credenciales son correctas
     * @throws UnauthorizedException si las credenciales son incorrectas
     * @throws BusinessRuleException si la cuenta está suspendida o inactiva
     */
    @Transactional
    public TokenResponse login(LoginRequest request, String deviceInfo) {
        // Buscar usuario por email (mismo mensaje si no existe o si contraseña es incorrecta)
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas"));

        // Verificar contraseña con BCrypt
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Credenciales inválidas");
        }

        // Verificar que la cuenta esté en condiciones de acceder
        if (user.getStatus() == EstadoGenerico.SUSPENDED) {
            throw new BusinessRuleException(
                    "Tu cuenta ha sido suspendida. Contacta soporte para más información.",
                    "CUENTA_SUSPENDIDA"
            );
        }

        if (user.getStatus() == EstadoGenerico.INACTIVE) {
            throw new BusinessRuleException(
                    "Tu cuenta está inactiva. Por favor actívala para continuar.",
                    "CUENTA_INACTIVA"
            );
        }

        return crearSesionYTokens(user, deviceInfo);
    }

    /**
     * Revoca la sesión activa del usuario (logout).
     *
     * Registra la fecha de revocación en la sesión. A partir de ese momento,
     * cualquier request con el access token de esa sesión será rechazado,
     * incluso si el token aún no ha expirado.
     *
     * @param sessionId UUID de la sesión a revocar (extraído del JWT por SecurityUtils)
     */
    @Transactional
    public void logout(UUID sessionId) {
        sessionRepository.findById(sessionId)
                .ifPresent(session -> {
                    session.setRevokedAt(Instant.now()); // Marcar como revocada
                    sessionRepository.save(session);
                });
        // Si la sesión no existe, se ignora silenciosamente (idempotente)
    }

    /**
     * Rota el refresh token y emite un nuevo par de tokens.
     *
     * Pasos:
     *  1. Hashea el refresh token recibido para buscarlo en la BD
     *  2. Verifica que la sesión exista, esté activa y no haya expirado
     *  3. Revoca la sesión antigua (invalida el refresh token usado)
     *  4. Crea una nueva sesión con nuevo refresh token
     *  5. Retorna el nuevo par de tokens
     *
     * Esto implementa rotación de refresh tokens: cada uso genera un nuevo token
     * e invalida el anterior. Si un token ya usado se vuelve a enviar, se rechaza.
     *
     * @param request Contiene el refresh token actual del cliente
     * @return Nuevo par de tokens (accessToken + refreshToken)
     * @throws UnauthorizedException si el refresh token es inválido, expirado o ya fue usado
     */
    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        // Hashear el refresh token para buscarlo en la BD
        String tokenHash = hashToken(request.refreshToken());

        // Buscar la sesión por hash
        Session session = sessionRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("El refresh token no es válido"));

        // Verificar que la sesión esté activa (no revocada y no expirada)
        if (!session.isActive()) {
            throw new UnauthorizedException("El refresh token ha expirado o ya fue utilizado");
        }

        // Cargar el usuario de la sesión
        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", session.getUserId().toString()));

        // Revocar la sesión antigua (el refresh token ya no puede usarse de nuevo)
        session.setRevokedAt(Instant.now());
        sessionRepository.save(session);

        // Crear nueva sesión y retornar nuevos tokens
        return crearSesionYTokens(user, session.getDeviceInfo());
    }

    /**
     * Crea una nueva sesión en la BD y genera el par de tokens correspondiente.
     *
     * Pasos internos:
     *  1. Genera un UUID como refresh token (lo que recibe el cliente)
     *  2. Hashea el UUID para almacenarlo en la BD de forma segura
     *  3. Persiste la sesión con el hash
     *  4. Genera el access token JWT con el sessionId
     *
     * @param user       Usuario para el que se crea la sesión
     * @param deviceInfo Información del dispositivo (User-Agent)
     * @return Par de tokens listo para enviarse al cliente
     */
    @Transactional
    public TokenResponse googleLogin(String idToken, String deviceInfo) {
        // Verify token with Google
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
        java.util.Map<?, ?> info;
        try {
            info = restTemplate.getForObject(url, java.util.Map.class);
        } catch (Exception e) {
            throw new UnauthorizedException("Token de Google inválido");
        }

        if (info == null) throw new UnauthorizedException("Token de Google inválido");

        String aud = String.valueOf(info.get("aud"));
        String emailVerified = String.valueOf(info.get("email_verified"));
        if (!googleClientId.isBlank() && !aud.equals(googleClientId))
            throw new UnauthorizedException("Token de Google no corresponde a esta aplicación");
        if (!"true".equals(emailVerified))
            throw new UnauthorizedException("El email de Google no está verificado");

        String email      = String.valueOf(info.get("email"));
        String firstName  = info.get("given_name") != null ? String.valueOf(info.get("given_name")) : "Usuario";
        String lastName   = info.get("family_name") != null ? String.valueOf(info.get("family_name")) : "";
        String avatarUrl  = info.get("picture") != null ? String.valueOf(info.get("picture")) : null;

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .firstName(firstName)
                    .lastName(lastName)
                    .avatarUrl(avatarUrl)
                    .role(RolUsuario.CLIENT)
                    .status(EstadoGenerico.ACTIVE)
                    .emailVerified(true)
                    .build();
            return userRepository.save(newUser);
        });

        if (user.getStatus() != EstadoGenerico.ACTIVE)
            throw new BusinessRuleException("Tu cuenta está suspendida", "CUENTA_SUSPENDIDA");

        // Update avatar if changed
        if (avatarUrl != null && !avatarUrl.equals(user.getAvatarUrl())) {
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);
        }

        return crearSesionYTokens(user, deviceInfo);
    }

    private TokenResponse crearSesionYTokens(User user, String deviceInfo) {
        // Generar refresh token como UUID aleatorio
        String refreshToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(refreshToken);

        // Crear y persistir la sesión
        Session session = Session.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .deviceInfo(deviceInfo)
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS)) // Expira en 30 días
                .build();

        session = sessionRepository.save(session);

        // Generar access token JWT con el ID de la sesión recién creada
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getRole().name(),
                user.getEmail(),
                session.getId()
        );

        return new TokenResponse(accessToken, refreshToken, user.getId(), user.getRole().name());
    }

    /**
     * Genera el hash SHA-256 de un token en Base64.
     *
     * Se usa para almacenar refresh tokens de forma segura:
     * el cliente guarda el token original, la BD solo guarda el hash.
     *
     * @param token Token en texto plano (ej: UUID del refresh token)
     * @return Hash SHA-256 codificado en Base64
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 siempre está disponible en Java, esto nunca debería ocurrir
            throw new IllegalStateException("No se pudo inicializar SHA-256", e);
        }
    }
}
