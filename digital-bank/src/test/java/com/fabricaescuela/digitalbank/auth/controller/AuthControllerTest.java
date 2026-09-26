package com.fabricaescuela.digitalbank.auth.controller;

import com.fabricaescuela.digitalbank.auth.dto.LoginRequest;
import com.fabricaescuela.digitalbank.auth.dto.LoginResponse;
import com.fabricaescuela.digitalbank.auth.exception.CredencialesInvalidasException;
import com.fabricaescuela.digitalbank.auth.interfaces.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController - endpoint de login")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("Un login correcto responde 200 con el token emitido")
    void loginCorrectoResponde200() {
        // Arrange
        LoginRequest request = new LoginRequest("ana@banco.com", "Password123");
        LoginResponse esperada = LoginResponse.of("jwt.de.prueba", 60L);
        when(authService.login(request)).thenReturn(esperada);

        // Act
        ResponseEntity<LoginResponse> respuesta = authController.login(request);

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(esperada);
    }

    @Test
    @DisplayName("El controlador propaga el error de credenciales para que lo traduzca el manejador global")
    void propagaElErrorDeCredenciales() {
        // Arrange
        LoginRequest request = new LoginRequest("ana@banco.com", "clave-incorrecta");
        when(authService.login(request)).thenThrow(new CredencialesInvalidasException());

        // Act & Assert
        assertThatThrownBy(() -> authController.login(request))
                .isInstanceOf(CredencialesInvalidasException.class);
    }
}
