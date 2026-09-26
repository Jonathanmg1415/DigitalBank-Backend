package com.fabricaescuela.digitalbank.auth.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoginResponse - construccion de la respuesta de login")
class LoginResponseTest {

    @Test
    @DisplayName("El tipo de token siempre se expone como Bearer")
    void elTipoDeTokenEsBearer() {
        // Arrange
        String token = "jwt.de.prueba";

        // Act
        LoginResponse response = LoginResponse.of(token, 60L);

        // Assert
        assertThat(response.token()).isEqualTo(token);
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInMinutes()).isEqualTo(60L);
    }
}
