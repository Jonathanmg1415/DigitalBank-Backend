package com.fabricaescuela.digitalbank.cuenta.service;

import com.fabricaescuela.digitalbank.cuenta.dto.DepositoRequest;
import com.fabricaescuela.digitalbank.cuenta.entity.OrigenDeposito;
import com.fabricaescuela.digitalbank.cuenta.exception.MontoInvalidoException;
import com.fabricaescuela.digitalbank.cuenta.repository.CuentaRepository;
import com.fabricaescuela.digitalbank.cuenta.repository.TransaccionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DepositoServiceMontoInvalidoTest {

    private static final UUID CUENTA_ID = UUID.randomUUID();

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private TransaccionRepository transaccionRepository;

    @InjectMocks
    private DepositoServiceImpl depositoService;

    @Test
    @DisplayName("El service rechaza un monto cero aunque no pase por la validacion del DTO")
    void registrarDeposito_montoCero_debeLanzarExcepcion() {
        MontoInvalidoException ex = assertThrows(
                MontoInvalidoException.class,
                () -> depositoService.registrarDeposito(CUENTA_ID, new DepositoRequest(BigDecimal.ZERO, OrigenDeposito.EFECTIVO))
        );

        assertEquals("Monto inválido", ex.getMessage());
        verifyNoInteractions(cuentaRepository, transaccionRepository);
    }

    @Test
    @DisplayName("El service rechaza un monto negativo aunque no pase por la validacion del DTO")
    void registrarDeposito_montoNegativo_debeLanzarExcepcion() {
        MontoInvalidoException ex = assertThrows(
                MontoInvalidoException.class,
                () -> depositoService.registrarDeposito(CUENTA_ID, new DepositoRequest(BigDecimal.valueOf(-1), OrigenDeposito.EFECTIVO))
        );

        assertEquals("Monto inválido", ex.getMessage());
        verifyNoInteractions(cuentaRepository, transaccionRepository);
    }

    @Test
    @DisplayName("El service rechaza un monto nulo")
    void registrarDeposito_montoNulo_debeLanzarExcepcion() {
        assertThrows(
                MontoInvalidoException.class,
                () -> depositoService.registrarDeposito(CUENTA_ID, new DepositoRequest(null, OrigenDeposito.EFECTIVO))
        );

        verifyNoInteractions(cuentaRepository, transaccionRepository);
    }
}
