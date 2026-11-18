package com.tienda.perfumes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Datos del checkout. Mismas reglas que CheckoutPage.jsx.
 * El precio NO viene del frontend: el backend lo toma de MySQL.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CrearPedidoRequest(

        @NotNull(message = "Falta el usuario del pedido.")
        Long usuarioId,

        @NotEmpty(message = "El carrito está vacío.")
        List<@Valid @NotNull(message = "Producto no válido.") ItemPedidoRequest> items,

        @NotNull(message = "Faltan los datos del cliente.")
        @Valid
        ClienteRequest cliente,

        @NotNull(message = "Faltan los datos de entrega.")
        @Valid
        EnvioRequest envio
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItemPedidoRequest(

            @NotNull(message = "Producto no válido.")
            Long id,

            @NotNull(message = "La cantidad es obligatoria.")
            @Min(value = 1, message = "La cantidad mínima es 1.")
            @Max(value = 99, message = "La cantidad máxima por producto es 99.")
            Integer cantidad
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ClienteRequest(

            @NotBlank(message = "El nombre es obligatorio.")
            @Size(max = 50, message = "El nombre no puede superar 50 caracteres.")
            String nombre,

            @NotBlank(message = "El apellido es obligatorio.")
            @Size(max = 100, message = "El apellido no puede superar 100 caracteres.")
            String apellido,

            @NotBlank(message = "El correo es obligatorio.")
            @Email(message = "Ingresa un correo válido.")
            @Size(max = 100, message = "El correo no puede superar 100 caracteres.")
            String correo
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EnvioRequest(

            @NotBlank(message = "La calle es obligatoria.")
            @Size(max = 300, message = "La calle no puede superar 300 caracteres.")
            String calle,

            @Size(max = 20, message = "El departamento no puede superar 20 caracteres.")
            String departamento,

            @NotBlank(message = "Debes seleccionar una región.")
            @Size(max = 100, message = "La región no puede superar 100 caracteres.")
            String region,

            @NotBlank(message = "Debes seleccionar una comuna.")
            @Size(max = 100, message = "La comuna no puede superar 100 caracteres.")
            String comuna,

            @Size(max = 300, message = "Las indicaciones no pueden superar 300 caracteres.")
            String indicaciones
    ) {}
}
