package com.tienda.perfumes.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Información general que muestra Swagger UI.
 * Documentación disponible en http://localhost:8080/swagger-ui.html
 *
 * Para probar rutas protegidas: hacer login, copiar el "token" de la respuesta,
 * presionar el botón "Authorize" y pegarlo (sin escribir "Bearer").
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI perfumesOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Perfumes Store API")
                        .version("v1")
                        .description("API REST de la tienda de perfumes: catálogo, categorías, "
                                + "usuarios, pedidos (boletas) y administración. "
                                + "Autenticación con JWT. DSY1104 - Evaluación Parcial 3.")
                        .contact(new Contact().name("Equipo Perfumes Store")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Servidor local de desarrollo")))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                                .name(ESQUEMA_JWT)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token obtenido en /api/v1/usuarios/login o /api/v1/admin/login")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }
}
