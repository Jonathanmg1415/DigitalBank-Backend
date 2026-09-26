package com.fabricaescuela.digitalbank.auth.service;

import com.fabricaescuela.digitalbank.auth.dto.LoginRequest;
import com.fabricaescuela.digitalbank.auth.dto.LoginResponse;
import com.fabricaescuela.digitalbank.auth.entity.EstadoSeguridad;
import com.fabricaescuela.digitalbank.auth.entity.Rol;
import com.fabricaescuela.digitalbank.auth.entity.Usuario;
import com.fabricaescuela.digitalbank.auth.exception.CredencialesInvalidasException;
import com.fabricaescuela.digitalbank.auth.exception.CuentaBloqueadaException;
import com.fabricaescuela.digitalbank.auth.repository.UsuarioRepository;
import com.fabricaescuela.digitalbank.core.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl - autenticacion de usuarios")
class AuthServiceImplTest {

    private static final String EMAIL = "ana@banco.com";
    private static final String PASSWORD = "Password123";
    private static final String HASH = "$2a$10$hashAlmacenado";
    private static final String TOKEN = "jwt.de.prueba";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("Credenciales correctas devuelven un token Bearer con su vigencia")
    void credencialesCorrectasDevuelvenTokenBearer() {
        // Arrange
        Usuario usuario = usuarioActivo();
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
        when(jwtService.generarToken(usuario)).thenReturn(TOKEN);
        when(jwtService.getExpirationMinutes()).thenReturn(60L);

        // Act
        LoginResponse response = authService.login(new LoginRequest(EMAIL, PASSWORD));

        // Assert
        assertThat(response.token()).isEqualTo(TOKEN);
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInMinutes()).isEqualTo(60L);
    }

    @Test
    @DisplayName("Un login exitoso reinicia los intentos fallidos acumulados y persiste el usuario")
    void loginExitosoReiniciaIntentosFallidos() {
        // Arrange
        Usuario usuario = usuarioActivo();
        usuario.registrarIntentoFallido();
        usuario.registrarIntentoFallido();
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
        when(jwtService.generarToken(usuario)).thenReturn(TOKEN);

        // Act
        authService.login(new LoginRequest(EMAIL, PASSWORD));

        // Assert
        assertThat(usuario.getIntentosFallidos()).isZero();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("Un correo inexistente responde 401 sin revelar si el usuario existe")
    void correoInexistenteResponde401() {
        // Arrange
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Credenciales inválidas")
                .extracting(ex -> ((CredencialesInvalidasException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Una contrasena incorrecta responde 401, registra el intento y no emite token")
    void passwordIncorrectaRegistraIntentoFallido() {
        // Arrange
        Usuario usuario = usuarioActivo();
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("otra-clave", HASH)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "otra-clave")))
                .isInstanceOf(CredencialesInvalidasException.class);

        assertThat(usuario.getIntentosFallidos()).isEqualTo(1);
        assertThat(usuario.getEstadoSeguridad()).isEqualTo(EstadoSeguridad.ACTIVO);
        verify(usuarioRepository).save(usuario);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("El tercer intento fallido consecutivo deja la cuenta bloqueada temporalmente")
    void tercerIntentoFallidoBloqueaLaCuenta() {
        // Arrange
        Usuario usuario = usuarioActivo();
        usuario.registrarIntentoFallido();
        usuario.registrarIntentoFallido();
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("otra-clave", HASH)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "otra-clave")))
                .isInstanceOf(CredencialesInvalidasException.class);

        assertThat(usuario.getEstadoSeguridad()).isEqualTo(EstadoSeguridad.BLOQUEADO_TEMPORAL);
        assertThat(usuario.estaBloqueado()).isTrue();
    }

    @Test
    @DisplayName("Una cuenta bloqueada responde 429 sin llegar a validar la contrasena")
    void cuentaBloqueadaResponde429() {
        // Arrange
        Usuario usuario = usuarioBloqueadoHace(10);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(CuentaBloqueadaException.class)
                .extracting(ex -> ((CuentaBloqueadaException) ex).getStatus())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        verifyNoInteractions(passwordEncoder, jwtService);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Si el bloqueo ya expiro, el usuario se desbloquea y puede autenticarse")
    void bloqueoExpiradoPermiteAutenticarse() {
        // Arrange
        Usuario usuario = usuarioBloqueadoHace(61);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
        when(jwtService.generarToken(usuario)).thenReturn(TOKEN);

        // Act
        LoginResponse response = authService.login(new LoginRequest(EMAIL, PASSWORD));

        // Assert
        assertThat(response.token()).isEqualTo(TOKEN);
        assertThat(usuario.getEstadoSeguridad()).isEqualTo(EstadoSeguridad.ACTIVO);
        assertThat(usuario.getIntentosFallidos()).isZero();
    }

    private Usuario usuarioActivo() {
        Usuario usuario = new Usuario(UUID.randomUUID(), EMAIL, HASH, Rol.CLIENTE);
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
        return usuario;
    }

    /** Usuario bloqueado con la fecha de bloqueo desplazada hacia el pasado. */
    private Usuario usuarioBloqueadoHace(long minutos) {
        Usuario usuario = usuarioActivo();
        usuario.registrarIntentoFallido();
        usuario.registrarIntentoFallido();
        usuario.registrarIntentoFallido();
        ReflectionTestUtils.setField(usuario, "fechaBloqueo", LocalDateTime.now().minusMinutes(minutos));
        return usuario;
    }
}
