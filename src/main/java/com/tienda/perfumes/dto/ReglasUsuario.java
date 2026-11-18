package com.tienda.perfumes.dto;

/**
 * Reglas compartidas entre los DTO de usuarios para no repetirlas.
 */
public final class ReglasUsuario {

    /** Solo se aceptan correos @duoc.cl, @profesor.duoc.cl o @gmail.com (igual que el frontend). */
    public static final String CORREO_PERMITIDO =
            "^[^@\\s]+@(duoc\\.cl|profesor\\.duoc\\.cl|gmail\\.com)$";

    public static final String MENSAJE_CORREO =
            "Solo se permiten correos @duoc.cl, @profesor.duoc.cl o @gmail.com.";

    private ReglasUsuario() {
    }
}
