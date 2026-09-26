package com.fabricaescuela.digitalbank.cliente.dto;

import com.fabricaescuela.digitalbank.cliente.entity.TipoDocumento;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica las restricciones declarativas del payload de registro, que son
 * las que producen el 400 antes de llegar al servicio.
 */
@DisplayName("ClienteRegistroRequest - validaciones del payload")
class ClienteRegistroRequestValidationTest {

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
    @DisplayName("Un payload completo y correcto no produce violaciones")
    void payloadCorrectoNoProduceViolaciones() {
        // Arrange
        ClienteRegistroRequest request = new ClienteRegistroRequest(
                TipoDocumento.CC, "1020304050", "Ana", "Gomez",
                LocalDate.now().minusYears(30), "3001234567", "ana@banco.com", "Password123");

        // Act
        Set<ConstraintViolation<ClienteRegistroRequest>> violaciones = validator.validate(request);

        // Assert
        assertThat(violaciones).isEmpty();
    }

    @Test
    @DisplayName("Un telefono de 9 digitos es rechazado por el patron de 10 digitos")
    void telefonoDeNueveDigitosEsRechazado() {
        // Arrange
        ClienteRegistroRequest request = new ClienteRegistroRequest(
                TipoDocumento.CC, "1020304050", "Ana", "Gomez",
                LocalDate.now().minusYears(30), "300123456", "ana@banco.com", "Password123");

        // Act
        Set<ConstraintViolation<ClienteRegistroRequest>> violaciones = validator.validate(request);

        // Assert
        assertThat(violaciones)
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .containsExactly("telefono");
        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("El teléfono debe tener 10 dígitos");
    }

    @Test
    @DisplayName("Un correo sin arroba es rechazado")
    void correoSinArrobaEsRechazado() {
        // Arrange
        ClienteRegistroRequest request = new ClienteRegistroRequest(
                TipoDocumento.CC, "1020304050", "Ana", "Gomez",
                LocalDate.now().minusYears(30), "3001234567", "ana-banco.com", "Password123");

        // Act
        Set<ConstraintViolation<ClienteRegistroRequest>> violaciones = validator.validate(request);

        // Assert
        assertThat(violaciones)
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .containsExactly("email");
    }

    @Test
    @DisplayName("Una contrasena de 7 caracteres es rechazada por longitud minima")
    void passwordDeSieteCaracteresEsRechazada() {
        // Arrange
        ClienteRegistroRequest request = new ClienteRegistroRequest(
                TipoDocumento.CC, "1020304050", "Ana", "Gomez",
                LocalDate.now().minusYears(30), "3001234567", "ana@banco.com", "Pass123");

        // Act
        Set<ConstraintViolation<ClienteRegistroRequest>> violaciones = validator.validate(request);

        // Assert
        assertThat(violaciones)
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .containsExactly("password");
    }

    @Test
    @DisplayName("Una fecha de nacimiento futura es rechazada")
    void fechaDeNacimientoFuturaEsRechazada() {
        // Arrange
        ClienteRegistroRequest request = new ClienteRegistroRequest(
                TipoDocumento.CC, "1020304050", "Ana", "Gomez",
                LocalDate.now().plusDays(1), "3001234567", "ana@banco.com", "Password123");

        // Act
        Set<ConstraintViolation<ClienteRegistroRequest>> violaciones = validator.validate(request);

        // Assert
        assertThat(violaciones)
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .containsExactly("fechaNacimiento");
    }

    @Test
    @DisplayName("Los campos obligatorios ausentes se reportan todos juntos")
    void camposObligatoriosAusentesSeReportanTodos() {
        // Arrange
        ClienteRegistroRequest request = new ClienteRegistroRequest(
                null, "  ", "", "", null, null, null, null);

        // Act
        Set<ConstraintViolation<ClienteRegistroRequest>> violaciones = validator.validate(request);

        // Assert
        assertThat(violaciones)
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .containsExactlyInAnyOrder("tipoDocumento", "numeroDocumento", "nombres", "apellidos",
                        "fechaNacimiento", "telefono", "email", "password");
    }
}
