package com.fabricaescuela.digitalbank.cuenta.service;

import com.fabricaescuela.digitalbank.cuenta.dto.DepositoRequest;
import com.fabricaescuela.digitalbank.cuenta.dto.TransaccionResponse;
import com.fabricaescuela.digitalbank.cuenta.entity.Cuenta;
import com.fabricaescuela.digitalbank.cuenta.entity.EstadoCuenta;
import com.fabricaescuela.digitalbank.cuenta.entity.OrigenDeposito;
import com.fabricaescuela.digitalbank.cuenta.entity.TipoCuenta;
import com.fabricaescuela.digitalbank.cuenta.entity.Transaccion;
import com.fabricaescuela.digitalbank.cuenta.exception.CuentaNoDisponibleException;
import com.fabricaescuela.digitalbank.cuenta.exception.CuentaNoEncontradaException;
import com.fabricaescuela.digitalbank.cuenta.repository.CuentaRepository;
import com.fabricaescuela.digitalbank.cuenta.repository.TransaccionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DepositoServiceImpl - registro de depositos")
class DepositoServiceImplTest {

    private static final UUID CUENTA_ID = UUID.randomUUID();
    private static final String NUMERO_CUENTA = "1234567890";

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private TransaccionRepository transaccionRepository;

    @Captor
    private ArgumentCaptor<Transaccion> transaccionCaptor;

    @InjectMocks
    private DepositoServiceImpl depositoService;

    @Test
    @DisplayName("Un deposito incrementa el saldo contable de la cuenta")
    void elDepositoIncrementaElSaldoContable() {
        // Arrange
        Cuenta cuenta = cuentaConSaldo(new BigDecimal("100000.00"));
        prepararPersistencia(cuenta);

        // Act
        depositoService.registrarDeposito(CUENTA_ID, new DepositoRequest(new BigDecimal("50000.00"), OrigenDeposito.EFECTIVO));

        // Assert
        assertThat(cuenta.getSaldoContable()).isEqualByComparingTo(new BigDecimal("150000.00"));
        verify(cuentaRepository).save(cuenta);
    }

    @Test
    @DisplayName("La transaccion registra el saldo antes y despues del deposito")
    void laTransaccionRegistraElSaldoAnteriorYElNuevo() {
        // Arrange
        Cuenta cuenta = cuentaConSaldo(new BigDecimal("100000.00"));
        prepararPersistencia(cuenta);

        // Act
        depositoService.registrarDeposito(CUENTA_ID, new DepositoRequest(new BigDecimal("50000.00"), OrigenDeposito.CHEQUE));

        // Assert
        verify(transaccionRepository).save(transaccionCaptor.capture());
        Transaccion transaccion = transaccionCaptor.getValue();
        assertThat(transaccion.getCuentaId()).isEqualTo(CUENTA_ID);
        assertThat(transaccion.getMonto()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(transaccion.getSaldoAnterior()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(transaccion.getSaldoNuevo()).isEqualByComparingTo(new BigDecimal("150000.00"));
        assertThat(transaccion.getOrigen()).isEqualTo(OrigenDeposito.CHEQUE);
    }

    @Test
    @DisplayName("La respuesta expone la transaccion COMPLETADA de tipo DEPOSITO")
    void laRespuestaExponeLaTransaccionCompletada() {
        // Arrange
        Cuenta cuenta = cuentaConSaldo(BigDecimal.ZERO);
        prepararPersistencia(cuenta);

        // Act
        TransaccionResponse response = depositoService.registrarDeposito(
                CUENTA_ID, new DepositoRequest(new BigDecimal("80000.00"), OrigenDeposito.OTRO));

        // Assert
        assertThat(response.cuentaId()).isEqualTo(CUENTA_ID);
        assertThat(response.tipo()).isEqualTo("DEPOSITO");
        assertThat(response.estado()).isEqualTo("COMPLETADA");
        assertThat(response.origen()).isEqualTo("OTRO");
        assertThat(response.monto()).isEqualByComparingTo(new BigDecimal("80000.00"));
        assertThat(response.saldoAnterior()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.saldoNuevo()).isEqualByComparingTo(new BigDecimal("80000.00"));
        assertThat(response.fechaHora()).isNotNull();
    }

    @Test
    @DisplayName("Depositar en una cuenta inexistente responde 404 y no registra transaccion")
    void cuentaInexistenteResponde404() {
        // Arrange
        when(cuentaRepository.findById(CUENTA_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> depositoService.registrarDeposito(
                CUENTA_ID, new DepositoRequest(new BigDecimal("50000.00"), OrigenDeposito.EFECTIVO)))
                .isInstanceOf(CuentaNoEncontradaException.class)
                .hasMessage("Cuenta no encontrada")
                .extracting(ex -> ((CuentaNoEncontradaException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(cuentaRepository, never()).save(any());
        verifyNoInteractions(transaccionRepository);
    }

    @Test
    @DisplayName("Depositar en una cuenta CERRADA responde 409 y no altera el saldo")
    void cuentaCerradaResponde409() {
        // Arrange
        Cuenta cuenta = cuentaConSaldo(new BigDecimal("100000.00"));
        ReflectionTestUtils.setField(cuenta, "estado", EstadoCuenta.CERRADA);
        when(cuentaRepository.findById(CUENTA_ID)).thenReturn(Optional.of(cuenta));

        // Act & Assert
        assertThatThrownBy(() -> depositoService.registrarDeposito(
                CUENTA_ID, new DepositoRequest(new BigDecimal("50000.00"), OrigenDeposito.EFECTIVO)))
                .isInstanceOf(CuentaNoDisponibleException.class)
                .hasMessage("Cuenta no disponible")
                .extracting(ex -> ((CuentaNoDisponibleException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        assertThat(cuenta.getSaldoContable()).isEqualByComparingTo(new BigDecimal("100000.00"));
        verify(cuentaRepository, never()).save(any());
        verifyNoInteractions(transaccionRepository);
    }

    @Test
    @DisplayName("Una cuenta BLOQUEADA si admite depositos porque no esta cerrada")
    void cuentaBloqueadaAdmiteDepositos() {
        // Arrange
        Cuenta cuenta = cuentaConSaldo(new BigDecimal("100000.00"));
        ReflectionTestUtils.setField(cuenta, "estado", EstadoCuenta.BLOQUEADA);
        prepararPersistencia(cuenta);

        // Act
        TransaccionResponse response = depositoService.registrarDeposito(
                CUENTA_ID, new DepositoRequest(new BigDecimal("1000.00"), OrigenDeposito.EFECTIVO));

        // Assert
        assertThat(response.saldoNuevo()).isEqualByComparingTo(new BigDecimal("101000.00"));
    }

    private Cuenta cuentaConSaldo(BigDecimal saldo) {
        Cuenta cuenta = new Cuenta(UUID.randomUUID(), NUMERO_CUENTA, TipoCuenta.AHORROS, saldo);
        ReflectionTestUtils.setField(cuenta, "id", CUENTA_ID);
        return cuenta;
    }

    /** Los repositorios devuelven la misma instancia que reciben, como hace JPA. */
    private void prepararPersistencia(Cuenta cuenta) {
        when(cuentaRepository.findById(CUENTA_ID)).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.save(cuenta)).thenReturn(cuenta);
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }
}
