package com.tienda.perfumes.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Formato único de error que devuelve toda la API.
 *
 * Ejemplo:
 * {
 *   "status": 400,
 *   "error": "El precio debe ser mayor a 0.",
 *   "errores": { "precio": "El precio debe ser mayor a 0." },
 *   "ruta": "/api/v1/productos",
 *   "fecha": "2026-11-16T17:30:00"
 * }
 *
 * El frontend ya lee la propiedad "error" para mostrar el mensaje.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String error,
        Map<String, String> errores,
        String ruta,
        LocalDateTime fecha
) {

    public static ErrorResponse de(int status, String error, String ruta) {
        return new ErrorResponse(status, error, null, ruta, LocalDateTime.now());
    }

    public static ErrorResponse deCampos(int status, String error,
                                         Map<String, String> errores, String ruta) {
        return new ErrorResponse(status, error, errores, ruta, LocalDateTime.now());
    }
}
