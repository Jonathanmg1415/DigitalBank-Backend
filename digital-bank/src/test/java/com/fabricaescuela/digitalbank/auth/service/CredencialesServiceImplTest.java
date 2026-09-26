package com.fabricaescuela.digitalbank.auth.service;

import com.fabricaescuela.digitalbank.auth.entity.Rol;
import com.fabricaescuela.digitalbank.auth.entity.Usuario;
import com.fabricaescuela.digitalbank.auth.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CredencialesServiceImpl - alta de credenciales del cliente")
class CredencialesServiceImplTest {

    private static final String PASSWORD_PLANA = "Password123";
    private static final String PASSWORD_HASH = "$2a$10$hashGenerado";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Captor
    private ArgumentCaptor<Usuario> usuarioCaptor;

    @InjectMocks
    private CredencialesServiceImpl credencialesService;

    @Test
    @DisplayName("La contrasena se guarda encriptada y nunca en texto plano")
    void laPasswordSeGuardaEncriptada() {
        // Arrange
        UUID clienteId = UUID.randomUUID();
        when(passwordEncoder.encode(PASSWORD_PLANA)).thenReturn(PASSWORD_HASH);

        // Act
        credencialesService.registrarCredencialesCliente(clienteId, "ana@banco.com", PASSWORD_PLANA);

        // Assert
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario guardado = usuarioCaptor.getValue();
        assertThat(guardado.getPasswordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(guardado.getPasswordHash()).isNotEqualTo(PASSWORD_PLANA);
    }

    @Test
    @DisplayName("El usuario creado queda asociado al cliente con rol CLIENTE")
    void elUsuarioQuedaAsociadoAlClienteConRolCliente() {
        // Arrange
        UUID clienteId = UUID.randomUUID();
        when(passwordEncoder.encode(PASSWORD_PLANA)).thenReturn(PASSWORD_HASH);

        // Act
        credencialesService.registrarCredencialesCliente(clienteId, "ana@banco.com", PASSWORD_PLANA);

        // Assert
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario guardado = usuarioCaptor.getValue();
        assertThat(guardado.getClienteId()).isEqualTo(clienteId);
        assertThat(guardado.getEmail()).isEqualTo("ana@banco.com");
        assertThat(guardado.getRol()).isEqualTo(Rol.CLIENTE);
    }
}
