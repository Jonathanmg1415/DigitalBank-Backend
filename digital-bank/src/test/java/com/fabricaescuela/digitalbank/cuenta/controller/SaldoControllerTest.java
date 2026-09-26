package com.fabricaescuela.digitalbank.cuenta.controller;

import com.fabricaescuela.digitalbank.cuenta.dto.SaldoResponse;
import com.fabricaescuela.digitalbank.cuenta.interfaces.SaldoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SaldoController - traduccion del contexto de seguridad")
class SaldoControllerTest {

    private static final String NUMERO_CUENTA = "1234567890";

    @Mock
    private SaldoService saldoService;

    @Captor
    private ArgumentCaptor<String> rolCaptor;

    @InjectMocks
    private SaldoController saldoController;

    @Test
    @DisplayName("La consulta responde 200 con el saldo de la cuenta")
    void laConsultaResponde200ConElSaldo() {
        // Arrange
        SaldoResponse esperado = new SaldoResponse(
                NUMERO_CUENTA, new BigDecimal("200000.00"), new BigDecimal("170000.00"));
        when(saldoService.consultarSaldo(anyString(), any(UUID.class), anyString())).thenReturn(esperado);

        // Act
        ResponseEntity<SaldoResponse> respuesta =
                saldoController.consultarSaldo(NUMERO_CUENTA, autenticacion(UUID.randomUUID(), "ROLE_CLIENTE"));

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(esperado);
    }

    @Test
    @DisplayName("El prefijo ROLE_ se elimina antes de delegar al servicio")
    void elPrefijoRoleSeEliminaAntesDeDelegar() {
        // Arrange
        UUID usuarioId = UUID.randomUUID();
        Authentication autenticacion = autenticacion(usuarioId, "ROLE_CAJERO");

        // Act
        saldoController.consultarSaldo(NUMERO_CUENTA, autenticacion);

        // Assert
        verify(saldoService).consultarSaldo(anyString(), any(UUID.class), rolCaptor.capture());
        assertThat(rolCaptor.getValue()).isEqualTo("CAJERO");
    }

    @Test
    @DisplayName("El id de usuario se toma del principal autenticado")
    void elIdDeUsuarioSeTomaDelPrincipal() {
        // Arrange
        UUID usuarioId = UUID.randomUUID();

        // Act
        saldoController.consultarSaldo(NUMERO_CUENTA, autenticacion(usuarioId, "ROLE_ADMIN"));

        // Assert
        verify(saldoService).consultarSaldo(NUMERO_CUENTA, usuarioId, "ADMIN");
    }

    @Test
    @DisplayName("Una autenticacion sin roles no llega a consultar el saldo")
    void autenticacionSinRolesNoConsultaElSaldo() {
        // Arrange
        Authentication sinRoles = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        // Act & Assert
        assertThatThrownBy(() -> saldoController.consultarSaldo(NUMERO_CUENTA, sinRoles))
                .isInstanceOf(NoSuchElementException.class);

        verifyNoInteractions(saldoService);
    }

    private Authentication autenticacion(UUID usuarioId, String authority) {
        return new UsernamePasswordAuthenticationToken(
                usuarioId, null, List.of(new SimpleGrantedAuthority(authority)));
    }
}
