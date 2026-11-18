package com.tienda.perfumes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o actualizar una categoría.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CategoriaRequest(

        @NotBlank(message = "El nombre de la categoría es obligatorio.")
        @Size(max = 50, message = "El nombre no puede superar 50 caracteres.")
        String nombre,

        @Size(max = 500, message = "La descripción no puede superar 500 caracteres.")
        String descripcion
) {
}
