package com.tienda.perfumes.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Validación de RUN chileno (módulo 11)")
class RutValidatorTest {

    private final RutValidator validador = new RutValidator();

    @ParameterizedTest(name = "{0} es un RUN válido")
    @ValueSource(strings = {"111111111", "123456785", "222222222", "760864285", "10000013K", "10000013k", "10000017"})
    void aceptaRunValidos(String rut) {
        assertThat(RutValidator.esValido(rut)).isTrue();
    }

    @ParameterizedTest(name = "{0} es un RUN inválido")
    @ValueSource(strings = {"123456789", "19011022K", "12.345.678-5", "12345678-5", "abc", "1234", "1234567890"})
    void rechazaRunInvalidos(String rut) {
        assertThat(RutValidator.esValido(rut)).isFalse();
    }

    @Test
    @DisplayName("Vacío o nulo se deja pasar: @NotBlank es quien lo exige")
    void vacioONuloSeDelegaANotBlank() {
        assertThat(validador.isValid(null, null)).isTrue();
        assertThat(validador.isValid("  ", null)).isTrue();
    }

    @Test
    @DisplayName("Ignora espacios al inicio y al final")
    void ignoraEspacios() {
        assertThat(validador.isValid("  111111111 ", null)).isTrue();
    }
}
