package com.fabricaescuela.digitalbank.cuenta.controller;

import com.fabricaescuela.digitalbank.cliente.entity.TipoDocumento;
import com.fabricaescuela.digitalbank.core.security.JwtService;
import com.fabricaescuela.digitalbank.cuenta.dto.AperturaCuentaRequest;
import com.fabricaescuela.digitalbank.cuenta.dto.CuentaResponse;
import com.fabricaescuela.digitalbank.cuenta.dto.DepositoRequest;
import com.fabricaescuela.digitalbank.cuenta.dto.TransaccionResponse;
import com.fabricaescuela.digitalbank.cuenta.entity.OrigenDeposito;
import com.fabricaescuela.digitalbank.cuenta.entity.TipoCuenta;
import com.fabricaescuela.digitalbank.cuenta.exception.ClienteNoAutorizadoException;
import com.fabricaescuela.digitalbank.cuenta.interfaces.CuentaService;
import com.fabricaescuela.digitalbank.cuenta.interfaces.DepositoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CuentaController - apertura de cuenta y depositos")
class CuentaControllerTest {

    private static final String TOKEN = "jwt.de.prueba";
    private static final String CABECERA_VALIDA = "Bearer " + TOKEN;

    @Mock
    private CuentaService cuentaService;

    @Mock
    private JwtService jwtService;

    @Mock
    private DepositoService depositoService;

    @InjectMocks
    private CuentaController cuentaController;

    @Test
    @DisplayName("La apertura responde 201 con la cuenta creada")
    void laAperturaResponde201() {
        // Arrange
        UUID clienteId = UUID.randomUUID();
        CuentaResponse esperada = cuentaResponse(clienteId);
        when(jwtService.extraerClienteId(TOKEN)).thenReturn(clienteId);
        when(cuentaService.abrirCuenta(any(AperturaCuentaRequest.class), any(UUID.class))).thenReturn(esperada);

        // Act
        ResponseEntity<CuentaResponse> respuesta =
                cuentaController.abrirCuenta(aperturaRequest(), CABECERA_VALIDA);

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isEqualTo(esperada);
    }

    @Test
    @DisplayName("El cliente autenticado se toma del token, no del cuerpo de la peticion")
    void elClienteAutenticadoSeTomaDelToken() {
        // Arrange
        UUID clienteIdDelToken = UUID.randomUUID();
        AperturaCuentaRequest request = aperturaRequest();
        when(jwtService.extraerClienteId(TOKEN)).thenReturn(clienteIdDelToken);
        when(cuentaService.abrirCuenta(request, clienteIdDelToken)).thenReturn(cuentaResponse(clienteIdDelToken));

        // Act
        ResponseEntity<CuentaResponse> respuesta = cuentaController.abrirCuenta(request, CABECERA_VALIDA);

        // Assert: si el controlador usara otro id, el stub no coincidiria y el cuerpo vendria vacio
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().clienteId()).isEqualTo(clienteIdDelToken);
    }

    @Test
    @DisplayName("Sin cabecera Authorization la apertura responde 403")
    void sinCabeceraLaAperturaResponde403() {
        // Arrange
        AperturaCuentaRequest request = aperturaRequest();

        // Act & Assert
        assertThatThrownBy(() -> cuentaController.abrirCuenta(request, null))
                .isInstanceOf(ClienteNoAutorizadoException.class)
                .hasMessage("No puedes abrir cuentas a nombre de otro cliente")
                .extracting(ex -> ((ClienteNoAutorizadoException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);

        verifyNoInteractions(jwtService, cuentaService);
    }

    @Test
    @DisplayName("Una cabecera con esquema distinto de Bearer responde 403")
    void cabeceraSinEsquemaBearerResponde403() {
        // Arrange
        AperturaCuentaRequest request = aperturaRequest();

        // Act & Assert
        assertThatThrownBy(() -> cuentaController.abrirCuenta(request, "Basic dXN1YXJpbzpjbGF2ZQ=="))
                .isInstanceOf(ClienteNoAutorizadoException.class);

        verifyNoInteractions(jwtService, cuentaService);
    }

    @Test
    @DisplayName("El deposito responde 201 con la transaccion registrada")
    void elDepositoResponde201() {
        // Arrange
        UUID cuentaId = UUID.randomUUID();
        DepositoRequest request = new DepositoRequest(new BigDecimal("50000.00"), OrigenDeposito.EFECTIVO);
        TransaccionResponse esperada = new TransaccionResponse(
                UUID.randomUUID(), cuentaId, "DEPOSITO", new BigDecimal("50000.00"),
                BigDecimal.ZERO, new BigDecimal("50000.00"), "EFECTIVO", "COMPLETADA", LocalDateTime.now());
        when(depositoService.registrarDeposito(cuentaId, request)).thenReturn(esperada);

        // Act
        ResponseEntity<TransaccionResponse> respuesta = cuentaController.depositar(cuentaId, request);

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isEqualTo(esperada);
    }

    private AperturaCuentaRequest aperturaRequest() {
        return new AperturaCuentaRequest(
                TipoDocumento.CC, "1020304050", TipoCuenta.AHORROS, new BigDecimal("150000.00"));
    }

    private CuentaResponse cuentaResponse(UUID clienteId) {
        return new CuentaResponse(
                UUID.randomUUID(), clienteId, "1234567890", TipoCuenta.AHORROS,
                new BigDecimal("150000.00"), BigDecimal.ZERO, new BigDecimal("150000.00"),
                "ACTIVA", LocalDateTime.now());
    }
}
