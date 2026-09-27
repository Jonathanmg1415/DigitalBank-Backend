package com.fabricaescuela.digitalbank.cuenta.service;

import com.fabricaescuela.digitalbank.cuenta.dto.DepositoRequest;
import com.fabricaescuela.digitalbank.cuenta.dto.TransaccionResponse;
import com.fabricaescuela.digitalbank.cuenta.entity.Cuenta;
import com.fabricaescuela.digitalbank.cuenta.entity.Transaccion;
import com.fabricaescuela.digitalbank.cuenta.exception.CuentaNoDisponibleException;
import com.fabricaescuela.digitalbank.cuenta.exception.CuentaNoEncontradaException;
import com.fabricaescuela.digitalbank.cuenta.exception.MontoInvalidoException;
import com.fabricaescuela.digitalbank.cuenta.interfaces.DepositoService;
import com.fabricaescuela.digitalbank.cuenta.repository.CuentaRepository;
import com.fabricaescuela.digitalbank.cuenta.repository.TransaccionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class DepositoServiceImpl implements DepositoService {

    private final CuentaRepository cuentaRepository;
    private final TransaccionRepository transaccionRepository;

    public DepositoServiceImpl(CuentaRepository cuentaRepository, TransaccionRepository transaccionRepository) {
        this.cuentaRepository = cuentaRepository;
        this.transaccionRepository = transaccionRepository;
    }

    @Override
    @Transactional
    public TransaccionResponse registrarDeposito(UUID cuentaId, DepositoRequest request) {
        validarMonto(request.monto());

        Cuenta cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(CuentaNoEncontradaException::new);

        validarCuentaDisponible(cuenta);

        BigDecimal saldoAnterior = cuenta.getSaldoContable();
        cuenta.acreditar(request.monto());
        Cuenta cuentaActualizada = cuentaRepository.save(cuenta);

        Transaccion transaccion = Transaccion.deposito(
                cuentaActualizada.getId(),
                request.monto(),
                saldoAnterior,
                cuentaActualizada.getSaldoContable(),
                request.origen()
        );
        Transaccion transaccionGuardada = transaccionRepository.save(transaccion);

        return TransaccionResponse.from(transaccionGuardada);
    }

    private void validarMonto(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new MontoInvalidoException();
        }
    }

    private void validarCuentaDisponible(Cuenta cuenta) {
        if (cuenta.estaCerrada()) {
            throw new CuentaNoDisponibleException("Cuenta no disponible");
        }
    }
}
