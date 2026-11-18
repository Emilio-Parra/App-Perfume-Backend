package com.tienda.perfumes.exception;

/**
 * Se lanza cuando se busca un registro que no existe en MySQL
 * (producto, categoría, usuario o pedido). Responde HTTP 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
