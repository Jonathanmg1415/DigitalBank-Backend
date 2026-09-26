package com.fabricaescuela.digitalbank.cliente.controller;

import com.fabricaescuela.digitalbank.cliente.dto.ClienteRegistroRequest;
import com.fabricaescuela.digitalbank.cliente.dto.ClienteResponse;
import com.fabricaescuela.digitalbank.cliente.entity.TipoDocumento;
import com.fabricaescuela.digitalbank.cliente.exception.ClienteYaExisteException;
import com.fabricaescuela.digitalbank.cliente.interfaces.ClienteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteController - endpoint de registro")
class ClienteControllerTest {

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private ClienteController clienteController;

    @Test
    @DisplayName("Un registro correcto responde 201 con el cliente creado")
    void registroCorrectoResponde201() {
        // Arrange
        ClienteRegistroRequest request = request();
        ClienteResponse esperada = new ClienteResponse(
                UUID.randomUUID(), TipoDocumento.CC, "1020304050", "Ana", "Gomez",
                LocalDate.now().minusYears(30), "3001234567", "ana@banco.com",
                "ACTIVO", LocalDateTime.now());
        when(clienteService.registrarCliente(request)).thenReturn(esperada);

        // Act
        ResponseEntity<ClienteResponse> respuesta = clienteController.registrar(request);

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isEqualTo(esperada);
    }

    @Test
    @DisplayName("El controlador propaga el conflicto de cliente duplicado")
    void propagaElConflictoDeClienteDuplicado() {
        // Arrange
        ClienteRegistroRequest request = request();
        when(clienteService.registrarCliente(request))
                .thenThrow(new ClienteYaExisteException("El cliente ya existe"));

        // Act & Assert
        assertThatThrownBy(() -> clienteController.registrar(request))
                .isInstanceOf(ClienteYaExisteException.class);
    }

    private ClienteRegistroRequest request() {
        return new ClienteRegistroRequest(
                TipoDocumento.CC, "1020304050", "Ana", "Gomez",
                LocalDate.now().minusYears(30), "3001234567", "ana@banco.com", "Password123");
    }
}
