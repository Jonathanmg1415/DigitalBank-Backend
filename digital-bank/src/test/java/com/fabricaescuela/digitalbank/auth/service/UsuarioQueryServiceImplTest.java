package com.fabricaescuela.digitalbank.auth.service;

import com.fabricaescuela.digitalbank.auth.entity.Rol;
import com.fabricaescuela.digitalbank.auth.entity.Usuario;
import com.fabricaescuela.digitalbank.auth.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioQueryServiceImpl - resolucion del cliente asociado")
class UsuarioQueryServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioQueryServiceImpl usuarioQueryService;

    @Test
    @DisplayName("Devuelve el cliente asociado al usuario autenticado")
    void devuelveElClienteAsociadoAlUsuario() {
        // Arrange
        UUID usuarioId = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(new Usuario(clienteId, "ana@banco.com", "hash", Rol.CLIENTE)));

        // Act
        Optional<UUID> resultado = usuarioQueryService.obtenerClienteId(usuarioId);

        // Assert
        assertThat(resultado).contains(clienteId);
    }

    @Test
    @DisplayName("Un usuario inexistente devuelve un resultado vacio")
    void usuarioInexistenteDevuelveVacio() {
        // Arrange
        UUID usuarioId = UUID.randomUUID();
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        // Act
        Optional<UUID> resultado = usuarioQueryService.obtenerClienteId(usuarioId);

        // Assert
        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Un usuario interno sin cliente asociado devuelve un resultado vacio")
    void usuarioInternoSinClienteDevuelveVacio() {
        // Arrange
        UUID usuarioId = UUID.randomUUID();
        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(new Usuario(null, "admin@banco.com", "hash", Rol.ADMIN)));

        // Act
        Optional<UUID> resultado = usuarioQueryService.obtenerClienteId(usuarioId);

        // Assert
        assertThat(resultado).isEmpty();
    }
}
