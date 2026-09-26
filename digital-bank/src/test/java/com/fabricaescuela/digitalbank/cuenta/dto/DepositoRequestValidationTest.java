package com.fabricaescuela.digitalbank.cuenta.dto;

import com.fabricaescuela.digitalbank.cuenta.entity.OrigenDeposito;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DepositoRequest - validaciones del monto y el origen")
class DepositoRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void abrirValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        factory.close();
    }

    @Test
    @DisplayName("Un monto positivo con origen definido es valido")
    void montoPositivoConOrigenEsValido() {
        // Arrange
        DepositoRequest request = new DepositoRequest(new BigDecimal("0.01"), OrigenDeposito.EFECTIVO);

        // Act
        Set<ConstraintViolation<DepositoRequest>> violaciones = validator.validate(request);

        // Assert
        assertThat(violaciones).isEmpty();
    }

    @Test
    @DisplayName("Un monto de cero es rechazado porque el minimo es exclusivo")
    void montoCeroEsRechazado() {
        // Arrange
        DepositoRequest request = new DepositoRequest(BigDecimal.ZERO, OrigenDeposito.EFECTIVO);

        // Act
        Set<ConstraintViolation<DepositoRequest>> violaciones = validator.validate(request);

        // Assert
        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Monto inválido");
    }

    @Test
    @DisplayName("Un monto negativo es rechazado")
    void montoNegativoEsRechazado() {
        // Arrange
        DepositoRequest request = new DepositoRequest(new BigDecimal("-1000"), OrigenDeposito.EFECTIVO);

        // Act
        Set<ConstraintViolation<DepositoRequest>> violaciones = validator.validate(request);

        // Assert
        assertThat(violaciones)
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .containsExactly("monto");
    }

    @Test
    @DisplayName("El monto y el origen son obligatorios")
    void montoYOrigenSonObligatorios() {
        // Arrange
        DepositoRequest request = new DepositoRequest(null, null);

        // Act
        Set<ConstraintViolation<DepositoRequest>> violaciones = validator.validate(request);

        // Assert
        assertThat(violaciones)
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .containsExactlyInAnyOrder("monto", "origen");
    }
}
