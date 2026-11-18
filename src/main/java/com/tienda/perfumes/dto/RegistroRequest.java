package com.tienda.perfumes.dto;

import com.tienda.perfumes.validation.RutValido;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registro público de un cliente. Mismas reglas que RegistroPage.jsx.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RegistroRequest(

        @NotBlank(message = "El RUN es obligatorio.")
        @RutValido
        String rut,

        @NotBlank(message = "El correo es obligatorio.")
        @Size(max = 100, message = "El correo no puede superar 100 caracteres.")
        @Pattern(regexp = ReglasUsuario.CORREO_PERMITIDO,
                message = ReglasUsuario.MENSAJE_CORREO)
        String correo,

        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 50, message = "El nombre no puede superar 50 caracteres.")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio.")
        @Size(max = 100, message = "El apellido no puede superar 100 caracteres.")
        String apellido,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 4, max = 10, message = "La contraseña debe tener entre 4 y 10 caracteres.")
        String pass,

        String fechaNac,

        @Size(max = 100, message = "La región no puede superar 100 caracteres.")
        String region,

        @Size(max = 100, message = "La comuna no puede superar 100 caracteres.")
        String comuna,

        @NotBlank(message = "La dirección es obligatoria.")
        @Size(max = 300, message = "La dirección no puede superar 300 caracteres.")
        String direccion
) {
}
