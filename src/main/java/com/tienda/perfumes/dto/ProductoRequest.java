package com.tienda.perfumes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o actualizar un perfume.
 * Mismas reglas que validarProducto() en ProductoForm.jsx.
 * Se usa un DTO para no recibir la entidad directamente: así el cliente
 * no puede enviar campos que no debe (por ejemplo el id).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductoRequest(

        @NotBlank(message = "La marca es obligatoria.")
        @Size(max = 100, message = "La marca no puede superar 100 caracteres.")
        String marca,

        @NotBlank(message = "El modelo es obligatorio.")
        @Size(max = 150, message = "El modelo no puede superar 150 caracteres.")
        String modelo,

        @NotBlank(message = "Selecciona una categoría.")
        @Size(max = 100, message = "La categoría no puede superar 100 caracteres.")
        String tipo,

        @Pattern(regexp = "(?i)hombre|mujer|unisex",
                message = "El género debe ser hombre, mujer o unisex.")
        String genero,

        @NotNull(message = "El precio es obligatorio.")
        @Positive(message = "El precio debe ser mayor a 0.")
        Integer precio,

        @Min(value = 0, message = "El descuento debe estar entre 0 y 90.")
        @Max(value = 90, message = "El descuento debe estar entre 0 y 90.")
        Integer descuento,

        @NotNull(message = "El stock es obligatorio.")
        @PositiveOrZero(message = "El stock debe ser 0 o mayor.")
        Integer stock,

        @NotBlank(message = "El volumen es obligatorio (ej. 100 ml).")
        @Size(max = 50, message = "El volumen no puede superar 50 caracteres.")
        String volumen,

        @NotBlank(message = "Selecciona una imagen para el perfume.")
        @Size(max = 1000, message = "La ruta de la imagen es demasiado larga.")
        String imagenes,

        @NotBlank(message = "La descripción es obligatoria.")
        @Size(min = 20, message = "La descripción debe tener al menos 20 caracteres.")
        String descripcion
) {
}
