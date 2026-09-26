package com.fabricaescuela.digitalbank.core.security;

import com.fabricaescuela.digitalbank.auth.entity.Rol;
import com.fabricaescuela.digitalbank.auth.entity.Usuario;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Se usa el JwtService real: la firma y el parseo son justamente la
 * conducta que interesa verificar.
 */
@DisplayName("JwtService - emision y lectura de tokens")
class JwtServiceTest {

    private static final String SECRETO = "secreto-de-pruebas-digital-bank-0123456789";
    private static final String OTRO_SECRETO = "otro-secreto-completamente-distinto-987654";

    private JwtService jwtService;

    @BeforeEach
    void crearServicio() {
        jwtService = new JwtService(SECRETO, 60);
    }

    @Test
    @DisplayName("El token de un cliente lleva su id de usuario, correo, rol y clienteId")
    void elTokenDeClienteLlevaSusDatos() {
        // Arrange
        UUID usuarioId = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        Usuario usuario = usuario(usuarioId, clienteId, "ana@banco.com", Rol.CLIENTE);

        // Act
        String token = jwtService.generarToken(usuario);

        // Assert
        Claims claims = jwtService.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo(usuarioId.toString());
        assertThat(claims.get("email", String.class)).isEqualTo("ana@banco.com");
        assertThat(claims.get("rol", String.class)).isEqualTo("CLIENTE");
        assertThat(jwtService.extraerUsuarioId(token)).isEqualTo(usuarioId);
        assertThat(jwtService.extraerRol(token)).isEqualTo("CLIENTE");
        assertThat(jwtService.extraerClienteId(token)).isEqualTo(clienteId);
    }

    @Test
    @DisplayName("El token de un usuario interno omite el clienteId")
    void elTokenDeUsuarioInternoOmiteElClienteId() {
        // Arrange
        Usuario admin = usuario(UUID.randomUUID(), null, "admin@banco.com", Rol.ADMIN);

        // Act
        String token = jwtService.generarToken(admin);

        // Assert
        assertThat(jwtService.parseToken(token).get("clienteId")).isNull();
        assertThat(jwtService.extraerClienteId(token)).isNull();
        assertThat(jwtService.extraerRol(token)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("El token lleva fecha de emision y de expiracion posterior")
    void elTokenLlevaFechaDeEmisionYExpiracion() {
        // Arrange
        Usuario usuario = usuario(UUID.randomUUID(), UUID.randomUUID(), "ana@banco.com", Rol.CLIENTE);

        // Act
        String token = jwtService.generarToken(usuario);

        // Assert
        Claims claims = jwtService.parseToken(token);
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    @DisplayName("Un token recien emitido es valido")
    void tokenRecienEmitidoEsValido() {
        // Arrange
        String token = jwtService.generarToken(usuario(UUID.randomUUID(), UUID.randomUUID(), "ana@banco.com", Rol.CLIENTE));

        // Act
        boolean valido = jwtService.esTokenValido(token);

        // Assert
        assertThat(valido).isTrue();
    }

    @Test
    @DisplayName("Un token firmado con otro secreto no es valido")
    void tokenFirmadoConOtroSecretoNoEsValido() {
        // Arrange
        JwtService servicioIntruso = new JwtService(OTRO_SECRETO, 60);
        String tokenAjeno = servicioIntruso.generarToken(
                usuario(UUID.randomUUID(), UUID.randomUUID(), "ana@banco.com", Rol.CLIENTE));

        // Act
        boolean valido = jwtService.esTokenValido(tokenAjeno);

        // Assert
        assertThat(valido).isFalse();
    }

    @Test
    @DisplayName("Un token expirado no es valido")
    void tokenExpiradoNoEsValido() {
        // Arrange: vigencia negativa, el token nace caducado
        JwtService servicioVencido = new JwtService(SECRETO, -1);
        String tokenVencido = servicioVencido.generarToken(
                usuario(UUID.randomUUID(), UUID.randomUUID(), "ana@banco.com", Rol.CLIENTE));

        // Act
        boolean valido = jwtService.esTokenValido(tokenVencido);

        // Assert
        assertThat(valido).isFalse();
    }

    @Test
    @DisplayName("Una cadena que no es un JWT no es valida")
    void cadenaQueNoEsJwtNoEsValida() {
        // Arrange
        String basura = "esto-no-es-un-token";

        // Act
        boolean valido = jwtService.esTokenValido(basura);

        // Assert
        assertThat(valido).isFalse();
    }

    @Test
    @DisplayName("Un token vacio no es valido")
    void tokenVacioNoEsValido() {
        // Arrange
        String vacio = "";

        // Act
        boolean valido = jwtService.esTokenValido(vacio);

        // Assert
        assertThat(valido).isFalse();
    }

    @Test
    @DisplayName("Un token manipulado en su carga no es valido")
    void tokenManipuladoNoEsValido() {
        // Arrange
        String token = jwtService.generarToken(usuario(UUID.randomUUID(), UUID.randomUUID(), "ana@banco.com", Rol.CLIENTE));
        String[] partes = token.split("\\.");
        String manipulado = partes[0] + "." + partes[1] + "X." + partes[2];

        // Act
        boolean valido = jwtService.esTokenValido(manipulado);

        // Assert
        assertThat(valido).isFalse();
    }

    @Test
    @DisplayName("La vigencia configurada se expone tal como se recibio")
    void laVigenciaConfiguradaSeExpone() {
        // Arrange
        JwtService servicio = new JwtService(SECRETO, 15);

        // Act
        long minutos = servicio.getExpirationMinutes();

        // Assert
        assertThat(minutos).isEqualTo(15);
    }

    private Usuario usuario(UUID usuarioId, UUID clienteId, String email, Rol rol) {
        Usuario usuario = new Usuario(clienteId, email, "hash", rol);
        ReflectionTestUtils.setField(usuario, "id", usuarioId);
        return usuario;
    }
}
