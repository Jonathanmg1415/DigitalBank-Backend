package com.fabricaescuela.digitalbank.core.security;

import com.fabricaescuela.digitalbank.auth.entity.Rol;
import com.fabricaescuela.digitalbank.auth.entity.Usuario;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * El filtro se ejercita con el JwtService real y con los dobles de
 * servlet de spring-test; lo unico simulado es la cadena de filtros.
 */
@DisplayName("JwtAuthenticationFilter - autenticacion por cabecera Authorization")
class JwtAuthenticationFilterTest {

    private static final String SECRETO = "secreto-de-pruebas-digital-bank-0123456789";

    private JwtService jwtService;
    private JwtAuthenticationFilter filtro;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain cadena;

    @BeforeEach
    void preparar() {
        jwtService = new JwtService(SECRETO, 60);
        filtro = new JwtAuthenticationFilter(jwtService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        cadena = mock(FilterChain.class);
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Un token valido autentica al usuario con su rol como ROLE_<rol>")
    void tokenValidoAutenticaAlUsuarioConSuRol() throws Exception {
        // Arrange
        UUID usuarioId = UUID.randomUUID();
        String token = jwtService.generarToken(usuario(usuarioId, Rol.CAJERO));
        request.addHeader("Authorization", "Bearer " + token);

        // Act
        filtro.doFilter(request, response, cadena);

        // Assert
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        assertThat(autenticacion).isNotNull();
        assertThat(autenticacion.getPrincipal()).isEqualTo(usuarioId);
        assertThat(autenticacion.getCredentials()).isNull();
        assertThat(autenticacion.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CAJERO");
        verify(cadena).doFilter(request, response);
    }

    @Test
    @DisplayName("Sin cabecera Authorization la peticion sigue sin autenticar")
    void sinCabeceraNoSeAutentica() throws Exception {
        // Arrange: la peticion no trae cabecera

        // Act
        filtro.doFilter(request, response, cadena);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(cadena).doFilter(request, response);
    }

    @Test
    @DisplayName("Un esquema distinto de Bearer se ignora")
    void esquemaDistintoDeBearerSeIgnora() throws Exception {
        // Arrange
        request.addHeader("Authorization", "Basic dXN1YXJpbzpjbGF2ZQ==");

        // Act
        filtro.doFilter(request, response, cadena);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(cadena).doFilter(request, response);
    }

    @Test
    @DisplayName("Un token invalido no autentica pero deja continuar la cadena")
    void tokenInvalidoNoAutenticaPeroContinuaLaCadena() throws Exception {
        // Arrange
        request.addHeader("Authorization", "Bearer token-falsificado");

        // Act
        filtro.doFilter(request, response, cadena);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(cadena).doFilter(request, response);
    }

    @Test
    @DisplayName("Un token firmado con otro secreto no autentica")
    void tokenDeOtroEmisorNoAutentica() throws Exception {
        // Arrange
        JwtService emisorIntruso = new JwtService("otro-secreto-completamente-distinto-987654", 60);
        String tokenAjeno = emisorIntruso.generarToken(usuario(UUID.randomUUID(), Rol.ADMIN));
        request.addHeader("Authorization", "Bearer " + tokenAjeno);

        // Act
        filtro.doFilter(request, response, cadena);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(cadena).doFilter(request, response);
    }

    private Usuario usuario(UUID usuarioId, Rol rol) {
        Usuario usuario = new Usuario(UUID.randomUUID(), "usuario@banco.com", "hash", rol);
        ReflectionTestUtils.setField(usuario, "id", usuarioId);
        return usuario;
    }
}
