package com.tienda.perfumes.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.random.RandomGenerator;

/**
 * Simula la respuesta de una pasarela de pago (Webpay, Mercado Pago, etc.).
 *
 * Está separado de PedidoService para que:
 *  - la regla del pago quede en un solo lugar, y
 *  - en los tests se pueda reemplazar por un simulador que siempre
 *    aprueba o siempre rechaza (el azar no se puede probar).
 *
 * La probabilidad se configura en application.properties con app.pago.probabilidad-exito.
 */
@Component
public class SimuladorPago {

    private final double probabilidadExito;
    private final RandomGenerator azar = RandomGenerator.getDefault();

    public SimuladorPago(@Value("${app.pago.probabilidad-exito:0.7}") double probabilidadExito) {
        if (probabilidadExito < 0 || probabilidadExito > 1) {
            throw new IllegalArgumentException("app.pago.probabilidad-exito debe estar entre 0 y 1.");
        }
        this.probabilidadExito = probabilidadExito;
    }

    /** @return true si el pago fue aprobado */
    public boolean aprobar() {
        return azar.nextDouble() < probabilidadExito;
    }
}
