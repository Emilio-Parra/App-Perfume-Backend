package com.tienda.perfumes.exception;

/**
 * Se lanza cuando la operación choca con el estado actual de los datos:
 * correo o RUT duplicado, stock insuficiente, pedido ya pagado,
 * categoría con productos asociados, etc. Responde HTTP 409.
 */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
