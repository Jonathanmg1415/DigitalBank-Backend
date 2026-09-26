package com.fabricaescuela.digitalbank.cuenta.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Cuenta - comportamiento de negocio")
class CuentaTest {

    private static final String NUMERO = "1234567890";

    @Test
    @DisplayName("Una cuenta creada sin monto de apertura nace ACTIVA con saldo y retencion en cero")
    void cuentaSinMontoNaceActivaEnCero() {
        // Arrange
        UUID clienteId = UUID.randomUUID();

        // Act
        Cuenta cuenta = new Cuenta(clienteId, NUMERO, TipoCuenta.AHORROS);

        // Assert
        assertThat(cuenta.getClienteId()).isEqualTo(clienteId);
        assertThat(cuenta.getNumeroCuenta()).isEqualTo(NUMERO);
        assertThat(cuenta.getTipoCuenta()).isEqualTo(TipoCuenta.AHORROS);
        assertThat(cuenta.getSaldoContable()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cuenta.getRetencion()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.ACTIVA);
        assertThat(cuenta.getFechaApertura()).isNotNull();
    }

    @Test
    @DisplayName("Una cuenta creada con monto de apertura conserva ese saldo inicial")
    void cuentaConMontoConservaElSaldoInicial() {
        // Arrange
        BigDecimal montoApertura = new BigDecimal("150000.00");

        // Act
        Cuenta cuenta = new Cuenta(UUID.randomUUID(), NUMERO, TipoCuenta.CORRIENTE, montoApertura);

        // Assert
        assertThat(cuenta.getSaldoContable()).isEqualByComparingTo(montoApertura);
        assertThat(cuenta.getTipoCuenta()).isEqualTo(TipoCuenta.CORRIENTE);
    }

    @Test
    @DisplayName("Acreditar suma el monto al saldo contable")
    void acreditarSumaElMontoAlSaldo() {
        // Arrange
        Cuenta cuenta = new Cuenta(UUID.randomUUID(), NUMERO, TipoCuenta.AHORROS, new BigDecimal("100000.00"));

        // Act
        cuenta.acreditar(new BigDecimal("25500.50"));

        // Assert
        assertThat(cuenta.getSaldoContable()).isEqualByComparingTo(new BigDecimal("125500.50"));
    }

    @Test
    @DisplayName("Acreditar varias veces acumula los montos")
    void acreditarVariasVecesAcumulaLosMontos() {
        // Arrange
        Cuenta cuenta = new Cuenta(UUID.randomUUID(), NUMERO, TipoCuenta.AHORROS);

        // Act
        cuenta.acreditar(new BigDecimal("10000"));
        cuenta.acreditar(new BigDecimal("5000"));

        // Assert
        assertThat(cuenta.getSaldoContable()).isEqualByComparingTo(new BigDecimal("15000"));
    }

    @Test
    @DisplayName("Una cuenta ACTIVA no esta cerrada")
    void cuentaActivaNoEstaCerrada() {
        // Arrange
        Cuenta cuenta = new Cuenta(UUID.randomUUID(), NUMERO, TipoCuenta.AHORROS);

        // Act
        boolean cerrada = cuenta.estaCerrada();

        // Assert
        assertThat(cerrada).isFalse();
    }

    @Test
    @DisplayName("Una cuenta BLOQUEADA no se considera cerrada")
    void cuentaBloqueadaNoEstaCerrada() {
        // Arrange
        Cuenta cuenta = new Cuenta(UUID.randomUUID(), NUMERO, TipoCuenta.AHORROS);
        ReflectionTestUtils.setField(cuenta, "estado", EstadoCuenta.BLOQUEADA);

        // Act
        boolean cerrada = cuenta.estaCerrada();

        // Assert
        assertThat(cerrada).isFalse();
    }

    @Test
    @DisplayName("Una cuenta CERRADA se reporta como cerrada")
    void cuentaCerradaSeReportaComoCerrada() {
        // Arrange
        Cuenta cuenta = new Cuenta(UUID.randomUUID(), NUMERO, TipoCuenta.AHORROS);
        ReflectionTestUtils.setField(cuenta, "estado", EstadoCuenta.CERRADA);

        // Act
        boolean cerrada = cuenta.estaCerrada();

        // Assert
        assertThat(cerrada).isTrue();
    }
}
