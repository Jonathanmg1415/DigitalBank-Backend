package com.fabricaescuela.digitalbank.core.exception;

import com.fabricaescuela.digitalbank.cliente.exception.ClienteMenorDeEdadException;
import com.fabricaescuela.digitalbank.cuenta.exception.CuentaNoEncontradaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler - cuerpo uniforme de error")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Una ApiException conserva su estado y su mensaje de negocio")
    void apiExceptionConservaEstadoYMensaje() {
        // Arrange
        MockHttpServletRequest request = peticion("/api/cuentas/1234567890/saldo");

        // Act
        ResponseEntity<Map<String, Object>> respuesta =
                handler.handleApiException(new CuentaNoEncontradaException(), request);

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody())
                .containsEntry("status", 404)
                .containsEntry("error", "Not Found")
                .containsEntry("message", "Cuenta no encontrada")
                .containsEntry("path", "/api/cuentas/1234567890/saldo");
        assertThat(respuesta.getBody().get("timestamp")).isInstanceOf(Instant.class);
    }

    @Test
    @DisplayName("Cada ApiException traduce su propio codigo HTTP")
    void cadaApiExceptionTraduceSuCodigoHttp() {
        // Arrange
        MockHttpServletRequest request = peticion("/api/clientes");

        // Act
        ResponseEntity<Map<String, Object>> respuesta = handler.handleApiException(
                new ClienteMenorDeEdadException("El cliente debe ser mayor de edad"), request);

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).containsEntry("message", "El cliente debe ser mayor de edad");
    }

    @Test
    @DisplayName("Un error de validacion responde 400 con el mensaje del primer campo invalido")
    void errorDeValidacionRespondeConElPrimerMensaje() throws Exception {
        // Arrange
        MockHttpServletRequest request = peticion("/api/clientes");
        MethodArgumentNotValidException excepcion = errorDeValidacion(
                "telefono", "El teléfono debe tener 10 dígitos");

        // Act
        ResponseEntity<Map<String, Object>> respuesta = handler.handleValidation(excepcion, request);

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody())
                .containsEntry("status", 400)
                .containsEntry("message", "El teléfono debe tener 10 dígitos")
                .containsEntry("path", "/api/clientes");
    }

    @Test
    @DisplayName("Un error de validacion sin campos concretos usa el mensaje genErico")
    void errorDeValidacionSinCamposUsaMensajeGenerico() throws Exception {
        // Arrange
        MockHttpServletRequest request = peticion("/api/clientes");
        MethodArgumentNotValidException excepcion = errorDeValidacion(null, null);

        // Act
        ResponseEntity<Map<String, Object>> respuesta = handler.handleValidation(excepcion, request);

        // Assert
        assertThat(respuesta.getBody()).containsEntry("message", "Datos inválidos");
    }

    @Test
    @DisplayName("Un JSON ilegible responde 400 sin filtrar detalles internos")
    void jsonIlegibleResponde400() {
        // Arrange
        MockHttpServletRequest request = peticion("/api/auth/login");
        HttpMessageNotReadableException excepcion = new HttpMessageNotReadableException(
                "Unexpected end-of-input at [Source: (String)\"{\"]",
                new MockHttpInputMessage("{".getBytes(StandardCharsets.UTF_8)));

        // Act
        ResponseEntity<Map<String, Object>> respuesta = handler.handleMensajeIlegible(excepcion, request);

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody())
                .containsEntry("message", "Solicitud inválida")
                .containsEntry("path", "/api/auth/login");
        assertThat(respuesta.getBody().get("message").toString()).doesNotContain("Source");
    }

    private MockHttpServletRequest peticion(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }

    /** Construye la excepcion tal como la levanta Spring al fallar @Valid. */
    private MethodArgumentNotValidException errorDeValidacion(String campo, String mensaje) throws Exception {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        if (campo != null) {
            bindingResult.addError(new FieldError("request", campo, null, false, null, null, mensaje));
        }
        MethodParameter parametro = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("metodoDeReferencia", String.class), 0);
        return new MethodArgumentNotValidException(parametro, bindingResult);
    }

    @SuppressWarnings("unused")
    private void metodoDeReferencia(String payload) {
        // Solo existe para construir un MethodParameter valido en las pruebas.
    }
}
