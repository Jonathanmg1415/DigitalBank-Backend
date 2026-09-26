package com.fabricaescuela.digitalbank.auth.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas del comportamiento de negocio del usuario: conteo de intentos
 * fallidos, bloqueo temporal y expiracion del bloqueo.
 */
@DisplayName("Usuario - reglas de bloqueo por intentos fallidos")
class UsuarioTest {

    @Test
    @DisplayName("Un usuario nuevo queda ACTIVO, sin intentos fallidos y sin bloqueo")
    void usuarioNuevoQuedaActivoYSinBloqueo() {
        // Arrange
        UUID clienteId = UUID.randomUUID();

        // Act
        Usuario usuario = new Usuario(clienteId, "ana@banco.com", "hash", Rol.CLIENTE);

        // Assert
        assertThat(usuario.getClienteId()).isEqualTo(clienteId);
        assertThat(usuario.getEmail()).isEqualTo("ana@banco.com");
        assertThat(usuario.getPasswordHash()).isEqualTo("hash");
        assertThat(usuario.getRol()).isEqualTo(Rol.CLIENTE);
        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getEstadoSeguridad()).isEqualTo(EstadoSeguridad.ACTIVO);
        assertThat(usuario.estaBloqueado()).isFalse();
    }

    @Test
    @DisplayName("Dos intentos fallidos acumulan el conteo pero no bloquean la cuenta")
    void dosIntentosFallidosNoBloqueanLaCuenta() {
        // Arrange
        Usuario usuario = nuevoUsuario();

        // Act
        usuario.registrarIntentoFallido();
        usuario.registrarIntentoFallido();

        // Assert
        assertThat(usuario.getIntentosFallidos()).isEqualTo(2);
        assertThat(usuario.getEstadoSeguridad()).isEqualTo(EstadoSeguridad.ACTIVO);
        assertThat(usuario.estaBloqueado()).isFalse();
    }

    @Test
    @DisplayName("El tercer intento fallido bloquea temporalmente la cuenta")
    void tercerIntentoFallidoBloqueaLaCuenta() {
        // Arrange
        Usuario usuario = nuevoUsuario();

        // Act
        usuario.registrarIntentoFallido();
        usuario.registrarIntentoFallido();
        usuario.registrarIntentoFallido();

        // Assert
        assertThat(usuario.getIntentosFallidos()).isEqualTo(3);
        assertThat(usuario.getEstadoSeguridad()).isEqualTo(EstadoSeguridad.BLOQUEADO_TEMPORAL);
        assertThat(usuario.estaBloqueado()).isTrue();
        assertThat(usuario.bloqueoExpiro()).isFalse();
    }

    @Test
    @DisplayName("El bloqueo sigue vigente 59 minutos despues de producirse")
    void bloqueoVigenteAntesDeUnaHora() {
        // Arrange
        Usuario usuario = usuarioBloqueadoHace(59);

        // Act
        boolean bloqueado = usuario.estaBloqueado();

        // Assert
        assertThat(bloqueado).isTrue();
        assertThat(usuario.bloqueoExpiro()).isFalse();
    }

    @Test
    @DisplayName("El bloqueo expira una vez transcurrida la hora")
    void bloqueoExpiraDespuesDeUnaHora() {
        // Arrange
        Usuario usuario = usuarioBloqueadoHace(61);

        // Act
        boolean bloqueado = usuario.estaBloqueado();

        // Assert
        assertThat(bloqueado).isFalse();
        assertThat(usuario.bloqueoExpiro()).isTrue();
    }

    @Test
    @DisplayName("Un usuario ACTIVO nunca reporta un bloqueo expirado")
    void usuarioActivoNoReportaBloqueoExpirado() {
        // Arrange
        Usuario usuario = nuevoUsuario();

        // Act
        boolean expiro = usuario.bloqueoExpiro();

        // Assert
        assertThat(expiro).isFalse();
    }

    @Test
    @DisplayName("Desbloquear reinicia el estado, el conteo y la fecha de bloqueo")
    void desbloquearReiniciaEstadoYConteo() {
        // Arrange
        Usuario usuario = usuarioBloqueadoHace(61);

        // Act
        usuario.desbloquear();

        // Assert
        assertThat(usuario.getEstadoSeguridad()).isEqualTo(EstadoSeguridad.ACTIVO);
        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.estaBloqueado()).isFalse();
        assertThat(ReflectionTestUtils.getField(usuario, "fechaBloqueo")).isNull();
    }

    @Test
    @DisplayName("Un login exitoso reinicia los intentos fallidos y registra la fecha")
    void loginExitosoReiniciaIntentosYRegistraFecha() {
        // Arrange
        Usuario usuario = nuevoUsuario();
        usuario.registrarIntentoFallido();
        usuario.registrarIntentoFallido();

        // Act
        usuario.registrarLoginExitoso();

        // Assert
        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getEstadoSeguridad()).isEqualTo(EstadoSeguridad.ACTIVO);
        assertThat(ReflectionTestUtils.getField(usuario, "fechaBloqueo")).isNull();
        assertThat(ReflectionTestUtils.getField(usuario, "ultimoLogin")).isNotNull();
    }

    @Test
    @DisplayName("Tras desbloquear, el conteo de intentos vuelve a empezar desde cero")
    void trasDesbloquearElConteoVuelveAEmpezar() {
        // Arrange
        Usuario usuario = usuarioBloqueadoHace(61);
        usuario.desbloquear();

        // Act
        usuario.registrarIntentoFallido();

        // Assert
        assertThat(usuario.getIntentosFallidos()).isEqualTo(1);
        assertThat(usuario.estaBloqueado()).isFalse();
    }

    private Usuario nuevoUsuario() {
        return new Usuario(UUID.randomUUID(), "ana@banco.com", "hash", Rol.CLIENTE);
    }

    /** Deja al usuario bloqueado y desplaza la fecha de bloqueo hacia el pasado. */
    private Usuario usuarioBloqueadoHace(long minutos) {
        Usuario usuario = nuevoUsuario();
        usuario.registrarIntentoFallido();
        usuario.registrarIntentoFallido();
        usuario.registrarIntentoFallido();
        ReflectionTestUtils.setField(usuario, "fechaBloqueo", LocalDateTime.now().minusMinutes(minutos));
        return usuario;
    }
}
