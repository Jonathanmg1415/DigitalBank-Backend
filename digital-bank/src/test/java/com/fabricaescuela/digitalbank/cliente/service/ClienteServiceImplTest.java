package com.fabricaescuela.digitalbank.cliente.service;

import com.fabricaescuela.digitalbank.auth.interfaces.CredencialesService;
import com.fabricaescuela.digitalbank.cliente.dto.ClienteRegistroRequest;
import com.fabricaescuela.digitalbank.cliente.dto.ClienteResponse;
import com.fabricaescuela.digitalbank.cliente.entity.Cliente;
import com.fabricaescuela.digitalbank.cliente.entity.TipoDocumento;
import com.fabricaescuela.digitalbank.cliente.exception.ClienteMenorDeEdadException;
import com.fabricaescuela.digitalbank.cliente.exception.ClienteYaExisteException;
import com.fabricaescuela.digitalbank.cliente.repository.ClienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteServiceImpl - registro de clientes")
class ClienteServiceImplTest {

    private static final String DOCUMENTO = "1020304050";
    private static final String EMAIL = "ana@banco.com";
    private static final String PASSWORD = "Password123";

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private CredencialesService credencialesService;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    @Test
    @DisplayName("Un cliente mayor de edad se registra ACTIVO con sus datos personales")
    void clienteMayorDeEdadSeRegistraActivo() {
        // Arrange
        LocalDate nacimiento = LocalDate.now().minusYears(30);
        ClienteRegistroRequest request = request(nacimiento);
        UUID idGenerado = UUID.randomUUID();
        when(clienteRepository.existsByTipoDocumentoAndNumeroDocumento(TipoDocumento.CC, DOCUMENTO)).thenReturn(false);
        when(clienteRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacion -> {
            Cliente cliente = invocacion.getArgument(0);
            ReflectionTestUtils.setField(cliente, "id", idGenerado);
            return cliente;
        });

        // Act
        ClienteResponse response = clienteService.registrarCliente(request);

        // Assert
        assertThat(response.id()).isEqualTo(idGenerado);
        assertThat(response.tipoDocumento()).isEqualTo(TipoDocumento.CC);
        assertThat(response.numeroDocumento()).isEqualTo(DOCUMENTO);
        assertThat(response.nombres()).isEqualTo("Ana");
        assertThat(response.apellidos()).isEqualTo("Gomez");
        assertThat(response.fechaNacimiento()).isEqualTo(nacimiento);
        assertThat(response.telefono()).isEqualTo("3001234567");
        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.estado()).isEqualTo("ACTIVO");
        assertThat(response.fechaRegistro()).isNotNull();
    }

    @Test
    @DisplayName("El registro crea las credenciales del cliente con la contrasena recibida")
    void elRegistroCreaLasCredencialesDelCliente() {
        // Arrange
        UUID idGenerado = UUID.randomUUID();
        when(clienteRepository.existsByTipoDocumentoAndNumeroDocumento(TipoDocumento.CC, DOCUMENTO)).thenReturn(false);
        when(clienteRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacion -> {
            Cliente cliente = invocacion.getArgument(0);
            ReflectionTestUtils.setField(cliente, "id", idGenerado);
            return cliente;
        });

        // Act
        clienteService.registrarCliente(request(LocalDate.now().minusYears(30)));

        // Assert
        verify(credencialesService).registrarCredencialesCliente(idGenerado, EMAIL, PASSWORD);
    }

    @Test
    @DisplayName("Un documento ya registrado responde 409 y no persiste nada")
    void documentoDuplicadoResponde409() {
        // Arrange
        when(clienteRepository.existsByTipoDocumentoAndNumeroDocumento(TipoDocumento.CC, DOCUMENTO)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> clienteService.registrarCliente(request(LocalDate.now().minusYears(30))))
                .isInstanceOf(ClienteYaExisteException.class)
                .hasMessage("El cliente ya existe")
                .extracting(ex -> ((ClienteYaExisteException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(clienteRepository, never()).save(any());
        verifyNoInteractions(credencialesService);
    }

    @Test
    @DisplayName("Un correo ya registrado responde 409 y no persiste nada")
    void emailDuplicadoResponde409() {
        // Arrange
        when(clienteRepository.existsByTipoDocumentoAndNumeroDocumento(TipoDocumento.CC, DOCUMENTO)).thenReturn(false);
        when(clienteRepository.existsByEmail(EMAIL)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> clienteService.registrarCliente(request(LocalDate.now().minusYears(30))))
                .isInstanceOf(ClienteYaExisteException.class);

        verify(clienteRepository, never()).save(any());
        verifyNoInteractions(credencialesService);
    }

    @Test
    @DisplayName("Un cliente de 17 anos responde 400 y no persiste nada")
    void clienteMenorDeEdadResponde400() {
        // Arrange
        when(clienteRepository.existsByTipoDocumentoAndNumeroDocumento(TipoDocumento.CC, DOCUMENTO)).thenReturn(false);
        when(clienteRepository.existsByEmail(EMAIL)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> clienteService.registrarCliente(request(LocalDate.now().minusYears(17))))
                .isInstanceOf(ClienteMenorDeEdadException.class)
                .hasMessage("El cliente debe ser mayor de edad")
                .extracting(ex -> ((ClienteMenorDeEdadException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(clienteRepository, never()).save(any());
        verifyNoInteractions(credencialesService);
    }

    @Test
    @DisplayName("Un cliente que cumple 18 anos hoy si puede registrarse")
    void clienteQueCumple18HoySiPuedeRegistrarse() {
        // Arrange
        when(clienteRepository.existsByTipoDocumentoAndNumeroDocumento(TipoDocumento.CC, DOCUMENTO)).thenReturn(false);
        when(clienteRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // Act
        ClienteResponse response = clienteService.registrarCliente(request(LocalDate.now().minusYears(18)));

        // Assert
        assertThat(response.estado()).isEqualTo("ACTIVO");
    }

    @Test
    @DisplayName("Un cliente al que le falta un dia para cumplir 18 anos es rechazado")
    void clienteUnDiaAntesDeCumplir18EsRechazado() {
        // Arrange
        LocalDate nacimiento = LocalDate.now().minusYears(18).plusDays(1);
        when(clienteRepository.existsByTipoDocumentoAndNumeroDocumento(TipoDocumento.CC, DOCUMENTO)).thenReturn(false);
        when(clienteRepository.existsByEmail(EMAIL)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> clienteService.registrarCliente(request(nacimiento)))
                .isInstanceOf(ClienteMenorDeEdadException.class);

        verify(clienteRepository, never()).save(any());
    }

    private ClienteRegistroRequest request(LocalDate fechaNacimiento) {
        return new ClienteRegistroRequest(
                TipoDocumento.CC, DOCUMENTO, "Ana", "Gomez",
                fechaNacimiento, "3001234567", EMAIL, PASSWORD);
    }
}
