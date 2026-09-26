package com.fabricaescuela.digitalbank.cuenta.service;

import com.fabricaescuela.digitalbank.cuenta.repository.CuentaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeneradorNumeroCuenta - numeros unicos de 10 digitos")
class GeneradorNumeroCuentaTest {

    private static final int MAX_INTENTOS = 25;

    @Mock
    private CuentaRepository cuentaRepository;

    @InjectMocks
    private GeneradorNumeroCuenta generador;

    @Test
    @DisplayName("Genera un numero de 10 digitos que no empieza en cero")
    void generaNumeroDeDiezDigitosQueNoEmpiezaEnCero() {
        // Arrange
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);

        // Act
        String numero = generador.generar();

        // Assert
        assertThat(numero).hasSize(10).containsOnlyDigits().doesNotStartWith("0");
    }

    @Test
    @DisplayName("Reintenta cuando el numero candidato ya existe")
    void reintentaCuandoElCandidatoYaExiste() {
        // Arrange: los dos primeros candidatos ya existen, el tercero no
        when(cuentaRepository.existsByNumeroCuenta(anyString()))
                .thenReturn(true, true, false);

        // Act
        String numero = generador.generar();

        // Assert
        assertThat(numero).hasSize(10);
        verify(cuentaRepository, times(3)).existsByNumeroCuenta(anyString());
    }

    @Test
    @DisplayName("Tras 25 colisiones consecutivas falla en lugar de devolver un numero repetido")
    void trasVeinticincoColisionesFalla() {
        // Arrange: todo candidato colisiona
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> generador.generar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No fue posible generar un numero de cuenta unico");

        verify(cuentaRepository, times(MAX_INTENTOS)).existsByNumeroCuenta(anyString());
    }

    @Test
    @DisplayName("Invocaciones sucesivas producen numeros distintos")
    void invocacionesSucesivasProducenNumerosDistintos() {
        // Arrange
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);

        // Act
        Set<String> numeros = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            numeros.add(generador.generar());
        }

        // Assert: con 9.000 millones de combinaciones, 50 sorteos no deberian repetirse
        assertThat(numeros).hasSize(50);
    }
}
