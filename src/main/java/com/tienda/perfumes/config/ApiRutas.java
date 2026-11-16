package com.tienda.perfumes.config;

/**
 * Prefijo común para el versionado de la API REST.
 * Si en el futuro hay cambios incompatibles, se crea V2
 * sin romper a los clientes que siguen usando V1.
 */
public final class ApiRutas {

    public static final String V1 = "/api/v1";

    private ApiRutas() {
    }
}
