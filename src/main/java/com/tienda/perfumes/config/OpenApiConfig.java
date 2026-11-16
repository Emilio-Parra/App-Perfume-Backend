package com.tienda.perfumes.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Información general que muestra Swagger UI.
 * Documentación disponible en http://localhost:8080/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI perfumesOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Perfumes Store API")
                        .version("v1")
                        .description("API REST de la tienda de perfumes: catálogo, categorías, "
                                + "usuarios, pedidos (boletas) y administración. "
                                + "DSY1104 - Evaluación Parcial 3.")
                        .contact(new Contact().name("Equipo Perfumes Store")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Servidor local de desarrollo")));
    }
}
