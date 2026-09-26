package com.fabricaescuela.digitalbank.cuenta.dto;

import com.fabricaescuela.digitalbank.cuenta.entity.Cuenta;
import com.fabricaescuela.digitalbank.cuenta.entity.EstadoCuenta;
import com.fabricaescuela.digitalbank.cuenta.entity.TipoCuenta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CuentaResponse - proyeccion de la cuenta")
class CuentaResponseTest {

    @Test
    @DisplayName("El saldo disponible se deriva restando la retencion al saldo contable")
    void elSaldoDisponibleDescuentaLaRetencion() {
        // Arrange
        Cuenta cuenta = new Cuenta(UUID.randomUUID(), "1234567890", TipoCuenta.AHORROS, new BigDecimal("200000.00"));
        ReflectionTestUtils.setField(cuenta, "retencion", new BigDecimal("30000.00"));

        // Act
        CuentaResponse response = CuentaResponse.from(cuenta);

        // Assert
        assertThat(response.saldoContable()).isEqualByComparingTo(new BigDecimal("200000.00"));
        assertThat(response.retencion()).isEqualByComparingTo(new BigDecimal("30000.00"));
        assertThat(response.saldoDisponible()).isEqualByComparingTo(new BigDecimal("170000.00"));
    }

    @Test
    @DisplayName("El estado de la cuenta se expone como texto")
    void elEstadoSeExponeComoTexto() {
        // Arrange
        Cuenta cuenta = new Cuenta(UUID.randomUUID(), "1234567890", TipoCuenta.CORRIENTE);
        ReflectionTestUtils.setField(cuenta, "estado", EstadoCuenta.BLOQUEADA);

        // Act
        CuentaResponse response = CuentaResponse.from(cuenta);

        // Assert
        assertThat(response.estado()).isEqualTo("BLOQUEADA");
        assertThat(response.tipoCuenta()).isEqualTo(TipoCuenta.CORRIENTE);
        assertThat(response.numeroCuenta()).isEqualTo("1234567890");
        assertThat(response.fechaApertura()).isNotNull();
    }
}
