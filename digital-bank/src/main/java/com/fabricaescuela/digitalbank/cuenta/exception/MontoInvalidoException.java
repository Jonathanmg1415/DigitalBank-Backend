package com.fabricaescuela.digitalbank.cuenta.exception;

import com.fabricaescuela.digitalbank.core.exception.ApiException;
import org.springframework.http.HttpStatus;

public class MontoInvalidoException extends ApiException {
    public MontoInvalidoException() {
        super(HttpStatus.BAD_REQUEST, "Monto inválido");
    }
}
