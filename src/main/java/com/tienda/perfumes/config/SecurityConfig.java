
package com.tienda.perfumes.config;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

@Configuration
public class SecurityConfig {

    private static final String API = ApiRutas.V1;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            // Configuración temporal para desarrollo.
            // La protección CSRF sigue pendiente.
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(API + "/**")
            )

            // Recuperar la autenticación de la sesión
            .securityContext(context -> context
                .securityContextRepository(
                    new HttpSessionSecurityContextRepository()
                )
            )

            .authorizeHttpRequests(auth -> auth

                // Documentación Swagger / OpenAPI
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()

                // Inicio de sesión de administrador
                .requestMatchers(
                    HttpMethod.POST, API + "/admin/login"
                ).permitAll()

                // Consultas privadas del administrador
                .requestMatchers(
                    API + "/pedidos/admin",
                    API + "/usuarios/total-clientes"
                ).hasRole("ADMIN")

                // Rutas administrativas
                .requestMatchers(
                    API + "/admin/**"
                ).hasRole("ADMIN")

                // Gestión de productos
                .requestMatchers(
                    HttpMethod.POST, API + "/productos/**"
                ).hasRole("ADMIN")

                .requestMatchers(
                    HttpMethod.PUT, API + "/productos/**"
                ).hasRole("ADMIN")

                .requestMatchers(
                    HttpMethod.DELETE, API + "/productos/**"
                ).hasRole("ADMIN")

                // Gestión de categorías (antes quedaba abierta a cualquiera)
                .requestMatchers(
                    HttpMethod.POST, API + "/categorias/**"
                ).hasRole("ADMIN")

                .requestMatchers(
                    HttpMethod.PUT, API + "/categorias/**"
                ).hasRole("ADMIN")

                .requestMatchers(
                    HttpMethod.DELETE, API + "/categorias/**"
                ).hasRole("ADMIN")

                // Crear pedidos: requiere sesión de cliente
                .requestMatchers(
                    HttpMethod.POST, API + "/pedidos"
                ).hasRole("CLIENTE")

                // Procesar pagos: requiere sesión de cliente
                .requestMatchers(
                    HttpMethod.POST, API + "/pedidos/*/pago"
                ).hasRole("CLIENTE")

                // Registro, login de clientes y catálogo
                .requestMatchers(API + "/**").permitAll()

                .anyRequest().denyAll()
            )

            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(
                    (request, response, error) ->
                        response.setStatus(
                            HttpServletResponse.SC_UNAUTHORIZED
                        )
                )
            );

        return http.build();
    }
}
