package com.fabricaescuela.digitalbank.cuenta.dto;

import com.fabricaescuela.digitalbank.cliente.entity.TipoDocumento;
import com.fabricaescuela.digitalbank.cuenta.entity.TipoCuenta;
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

@DisplayName("AperturaCuentaRequest - validaciones del payload de apertura")
class AperturaCuentaRequestValidationTest {

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
    @DisplayName("Un payload completo es valido")
    void payloadCompletoEsValido() {
        // Arrange
        AperturaCuentaRequest request = new AperturaCuentaRequest(
                TipoDocumento.CC, "1020304050", TipoCuenta.AHORROS, new BigDecimal("150000.00"));

        // Act
        Set<ConstraintViolation<AperturaCuentaRequest>> violaciones = validator.validate(request);

        // Assert
        assertThat(violaciones).isEmpty();
    }

    @Test
    @DisplayName("El monto de apertura es opcional y puede omitirse")
    void elMontoDeAperturaEsOpcional() {
        // Arrange
        AperturaCuentaRequest request = new AperturaCuentaRequest(
                TipoDocumento.CC, "1020304050", TipoCuenta.AHORROS, null);

        // Act
        Set<ConstraintViolation<AperturaCuentaRequest>> violaciones = validator.validate(request);

        // Assert
        assertThat(violaciones).isEmpty();
    }

    @Test
    @DisplayName("Un monto de apertura negativo es rechazado")
    void montoDeAperturaNegativoEsRechazado() {
        // Arrange
        AperturaCuentaRequest request = new AperturaCuentaRequest(
                TipoDocumento.CC, "1020304050", TipoCuenta.AHORROS, new BigDecimal("-1"));

        // Act
        Set<ConstraintViolation<AperturaCuentaRequest>> violaciones = validator.validate(request);

        // Assert
        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("El monto de apertura no puede ser negativo");
    }

    @Test
    @DisplayName("El tipo de documento, el numero y el tipo de cuenta son obligatorios")
    void losCamposIdentificatoriosSonObligatorios() {
        // Arrange
        AperturaCuentaRequest request = new AperturaCuentaRequest(null, "   ", null, BigDecimal.ZERO);

        // Act
        Set<ConstraintViolation<AperturaCuentaRequest>> violaciones = validator.validate(request);

        // Assert
        assertThat(violaciones)
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .containsExactlyInAnyOrder("tipoDocumento", "numeroDocumento", "tipoCuenta");
    }
}
