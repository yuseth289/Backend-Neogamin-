package com.neogaming.auth.service;

import com.neogaming.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementación de UserDetailsService para Spring Security.
 *
 * Spring Security requiere esta interfaz para cargar los datos del usuario
 * durante el proceso de autenticación (login con email y contraseña).
 *
 * Flujo de uso:
 *  1. AuthController recibe email + contraseña del cliente
 *  2. AuthService llama al AuthenticationManager con las credenciales
 *  3. AuthenticationManager usa DaoAuthenticationProvider
 *  4. DaoAuthenticationProvider llama a loadUserByUsername(email)
 *  5. Este servicio busca el usuario en la BD y retorna un UserDetails
 *  6. DaoAuthenticationProvider verifica la contraseña con BCrypt
 *
 * Nota sobre el campo "username" de UserDetails:
 *  Spring llama al parámetro "username" pero en NeoGaming el identificador
 *  de acceso es el email. El campo se usa con el userId (UUID como String)
 *  para que sea consistente con lo que se almacena en el JWT.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Carga los datos del usuario por su email para que Spring Security
     * pueda verificar la contraseña durante el login.
     *
     * @param email Email del usuario (Spring lo pasa como "username")
     * @return UserDetails con el userId, hash de contraseña y rol del usuario
     * @throws UsernameNotFoundException si no existe un usuario con ese email
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(user -> new User(
                        user.getId().toString(),           // Usamos el UUID como "username" interno
                        user.getPasswordHash(),            // Hash BCrypt (Spring lo compara con la contraseña recibida)
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())) // Rol como autoridad
                ))
                .orElseThrow(() ->
                        new UsernameNotFoundException("No se encontró usuario con email: " + email));
    }
}
