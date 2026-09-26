package com.fabricaescuela.digitalbank.cuenta.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Transaccion - creacion de un deposito")
class TransaccionTest {

    @Test
    @DisplayName("Un deposito queda COMPLETADO, tipado como DEPOSITO y con el rastro de saldos")
    void depositoQuedaCompletadoConElRastroDeSaldos() {
        // Arrange
        UUID cuentaId = UUID.randomUUID();
        BigDecimal monto = new BigDecimal("50000.00");
        BigDecimal saldoAnterior = new BigDecimal("100000.00");
        BigDecimal saldoNuevo = new BigDecimal("150000.00");
        LocalDateTime antesDeCrear = LocalDateTime.now();

        // Act
        Transaccion transaccion = Transaccion.deposito(
                cuentaId, monto, saldoAnterior, saldoNuevo, OrigenDeposito.EFECTIVO);

        // Assert
        assertThat(transaccion.getCuentaId()).isEqualTo(cuentaId);
        assertThat(transaccion.getTipo()).isEqualTo(TipoTransaccion.DEPOSITO);
        assertThat(transaccion.getMonto()).isEqualByComparingTo(monto);
        assertThat(transaccion.getSaldoAnterior()).isEqualByComparingTo(saldoAnterior);
        assertThat(transaccion.getSaldoNuevo()).isEqualByComparingTo(saldoNuevo);
        assertThat(transaccion.getOrigen()).isEqualTo(OrigenDeposito.EFECTIVO);
        assertThat(transaccion.getEstado()).isEqualTo(EstadoTransaccion.COMPLETADA);
        assertThat(transaccion.getFechaHora()).isAfterOrEqualTo(antesDeCrear);
    }

    @Test
    @DisplayName("El origen recibido se conserva sin sustituirse por un valor por defecto")
    void elOrigenRecibidoSeConserva() {
        // Arrange
        UUID cuentaId = UUID.randomUUID();

        // Act
        Transaccion transaccion = Transaccion.deposito(
                cuentaId, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, OrigenDeposito.CHEQUE);

        // Assert
        assertThat(transaccion.getOrigen()).isEqualTo(OrigenDeposito.CHEQUE);
    }
}
