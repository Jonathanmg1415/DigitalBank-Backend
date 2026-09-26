package com.fabricaescuela.digitalbank.cuenta.controller;

import com.fabricaescuela.digitalbank.core.security.JwtService;
import com.fabricaescuela.digitalbank.cuenta.dto.AperturaCuentaRequest;
import com.fabricaescuela.digitalbank.cuenta.dto.CuentaResponse;
import com.fabricaescuela.digitalbank.cuenta.dto.DepositoRequest;
import com.fabricaescuela.digitalbank.cuenta.dto.TransaccionResponse;
import com.fabricaescuela.digitalbank.cuenta.exception.ClienteNoAutorizadoException;
import com.fabricaescuela.digitalbank.cuenta.interfaces.CuentaService;
import com.fabricaescuela.digitalbank.cuenta.interfaces.DepositoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cuentas")
public class CuentaController {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final CuentaService cuentaService;
    private final JwtService jwtService;
    private final DepositoService depositoService;

    public CuentaController(CuentaService cuentaService, JwtService jwtService, DepositoService depositoService) {
        this.cuentaService = cuentaService;
        this.jwtService = jwtService;
        this.depositoService = depositoService;
    }

    @PostMapping
    public ResponseEntity<CuentaResponse> abrirCuenta(
            @Valid @RequestBody AperturaCuentaRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {

        UUID clienteIdAutenticado = extraerClienteId(authorizationHeader);
        CuentaResponse response = cuentaService.abrirCuenta(request, clienteIdAutenticado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    @PostMapping("/{cuentaId}/depositos")
    public ResponseEntity<TransaccionResponse> depositar(@PathVariable UUID cuentaId,
                                                            @Valid @RequestBody DepositoRequest request) {
        TransaccionResponse response = depositoService.registrarDeposito(cuentaId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private UUID extraerClienteId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(PREFIJO_BEARER)) {
            throw new ClienteNoAutorizadoException();
        }
        return jwtService.extraerClienteId(authorizationHeader.substring(PREFIJO_BEARER.length()));
    }
}