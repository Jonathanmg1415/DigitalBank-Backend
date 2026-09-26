package com.fabricaescuela.digitalbank.cuenta.service;

import com.fabricaescuela.digitalbank.auth.interfaces.UsuarioQueryService;
import com.fabricaescuela.digitalbank.core.exception.AccesoNoAutorizadoException;
import com.fabricaescuela.digitalbank.cuenta.dto.SaldoResponse;
import com.fabricaescuela.digitalbank.cuenta.entity.Cuenta;
import com.fabricaescuela.digitalbank.cuenta.entity.TipoCuenta;
import com.fabricaescuela.digitalbank.cuenta.exception.CuentaNoEncontradaException;
import com.fabricaescuela.digitalbank.cuenta.repository.CuentaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SaldoServiceImpl - consulta de saldo y control de acceso")
class SaldoServiceImplTest {

    private static final String NUMERO_CUENTA = "1234567890";
    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID CLIENTE_TITULAR = UUID.randomUUID();
    private static final UUID OTRO_CLIENTE = UUID.randomUUID();

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private UsuarioQueryService usuarioQueryService;

    @InjectMocks
    private SaldoServiceImpl saldoService;

    @Test
    @DisplayName("El saldo disponible se calcula como saldo contable menos retencion")
    void elSaldoDisponibleDescuentaLaRetencion() {
        // Arrange
        Cuenta cuenta = cuenta(new BigDecimal("200000.00"), new BigDecimal("30000.00"), CLIENTE_TITULAR);
        when(cuentaRepository.findByNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.of(cuenta));

        // Act
        SaldoResponse response = saldoService.consultarSaldo(NUMERO_CUENTA, USUARIO_ID, "ADMIN");

        // Assert
        assertThat(response.numeroCuenta()).isEqualTo(NUMERO_CUENTA);
        assertThat(response.saldoContable()).isEqualByComparingTo(new BigDecimal("200000.00"));
        assertThat(response.saldoDisponible()).isEqualByComparingTo(new BigDecimal("170000.00"));
    }

    @Test
    @DisplayName("Un ADMIN consulta cualquier cuenta sin verificar titularidad")
    void adminConsultaCualquierCuenta() {
        // Arrange
        Cuenta cuenta = cuenta(new BigDecimal("50000.00"), BigDecimal.ZERO, OTRO_CLIENTE);
        when(cuentaRepository.findByNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.of(cuenta));

        // Act
        SaldoResponse response = saldoService.consultarSaldo(NUMERO_CUENTA, USUARIO_ID, "ADMIN");

        // Assert
        assertThat(response.saldoDisponible()).isEqualByComparingTo(new BigDecimal("50000.00"));
        verifyNoInteractions(usuarioQueryService);
    }

    @Test
    @DisplayName("Un CAJERO consulta cualquier cuenta sin verificar titularidad")
    void cajeroConsultaCualquierCuenta() {
        // Arrange
        Cuenta cuenta = cuenta(new BigDecimal("50000.00"), BigDecimal.ZERO, OTRO_CLIENTE);
        when(cuentaRepository.findByNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.of(cuenta));

        // Act
        SaldoResponse response = saldoService.consultarSaldo(NUMERO_CUENTA, USUARIO_ID, "CAJERO");

        // Assert
        assertThat(response.saldoContable()).isEqualByComparingTo(new BigDecimal("50000.00"));
        verifyNoInteractions(usuarioQueryService);
    }

    @Test
    @DisplayName("El cliente titular consulta el saldo de su propia cuenta")
    void clienteTitularConsultaSuPropiaCuenta() {
        // Arrange
        Cuenta cuenta = cuenta(new BigDecimal("75000.00"), new BigDecimal("5000.00"), CLIENTE_TITULAR);
        when(cuentaRepository.findByNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.of(cuenta));
        when(usuarioQueryService.obtenerClienteId(USUARIO_ID)).thenReturn(Optional.of(CLIENTE_TITULAR));

        // Act
        SaldoResponse response = saldoService.consultarSaldo(NUMERO_CUENTA, USUARIO_ID, "CLIENTE");

        // Assert
        assertThat(response.saldoDisponible()).isEqualByComparingTo(new BigDecimal("70000.00"));
    }

    @Test
    @DisplayName("Un cliente que no es titular recibe 403")
    void clienteNoTitularRecibe403() {
        // Arrange
        Cuenta cuenta = cuenta(new BigDecimal("75000.00"), BigDecimal.ZERO, OTRO_CLIENTE);
        when(cuentaRepository.findByNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.of(cuenta));
        when(usuarioQueryService.obtenerClienteId(USUARIO_ID)).thenReturn(Optional.of(CLIENTE_TITULAR));

        // Act & Assert
        assertThatThrownBy(() -> saldoService.consultarSaldo(NUMERO_CUENTA, USUARIO_ID, "CLIENTE"))
                .isInstanceOf(AccesoNoAutorizadoException.class)
                .hasMessage("Acceso no autorizado")
                .extracting(ex -> ((AccesoNoAutorizadoException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Un usuario sin cliente asociado recibe 403 al consultar como CLIENTE")
    void usuarioSinClienteAsociadoRecibe403() {
        // Arrange
        Cuenta cuenta = cuenta(new BigDecimal("75000.00"), BigDecimal.ZERO, CLIENTE_TITULAR);
        when(cuentaRepository.findByNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.of(cuenta));
        when(usuarioQueryService.obtenerClienteId(USUARIO_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> saldoService.consultarSaldo(NUMERO_CUENTA, USUARIO_ID, "CLIENTE"))
                .isInstanceOf(AccesoNoAutorizadoException.class);
    }

    @Test
    @DisplayName("Para el personal autorizado una cuenta inexistente responde 404")
    void cuentaInexistenteResponde404AlPersonalAutorizado() {
        // Arrange
        when(cuentaRepository.findByNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> saldoService.consultarSaldo(NUMERO_CUENTA, USUARIO_ID, "ADMIN"))
                .isInstanceOf(CuentaNoEncontradaException.class)
                .extracting(ex -> ((CuentaNoEncontradaException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Para un cliente una cuenta inexistente responde 403 y no revela su existencia")
    void cuentaInexistenteResponde403AlCliente() {
        // Arrange
        when(cuentaRepository.findByNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> saldoService.consultarSaldo(NUMERO_CUENTA, USUARIO_ID, "CLIENTE"))
                .isInstanceOf(AccesoNoAutorizadoException.class);

        verifyNoInteractions(usuarioQueryService);
    }

    private Cuenta cuenta(BigDecimal saldoContable, BigDecimal retencion, UUID clienteId) {
        Cuenta cuenta = new Cuenta(clienteId, NUMERO_CUENTA, TipoCuenta.AHORROS, saldoContable);
        ReflectionTestUtils.setField(cuenta, "retencion", retencion);
        return cuenta;
    }
}
