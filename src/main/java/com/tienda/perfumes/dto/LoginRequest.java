package com.tienda.perfumes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para iniciar sesión (cliente o administrador).
 * No se validan reglas de formato para no dar pistas a quien intente adivinar cuentas.
 */
public record LoginRequest(

        @NotBlank(message = "El correo es obligatorio.")
        @Size(max = 100, message = "El correo no puede superar 100 caracteres.")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(max = 100, message = "La contraseña es demasiado larga.")
        String pass
) {
}
