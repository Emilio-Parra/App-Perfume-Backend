
package com.tienda.perfumes.config;

import com.tienda.perfumes.security.JwtAuthenticationFilter;
import com.tienda.perfumes.security.JwtService;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Seguridad de la API con JWT y roles.
 *
 * - Sin sesiones en el servidor (STATELESS): cada petición trae su token
 *   en el encabezado  Authorization: Bearer <token>
 * - JwtAuthenticationFilter valida el token y registra el rol.
 * - Aquí se define qué rol puede usar cada ruta. Además, @EnableMethodSecurity
 *   activa @PreAuthorize en los controladores como segunda capa de control.
 *
 * Roles:
 *   ADMIN    -> acceso total
 *   VENDEDOR -> solo puede ver productos (público) y pedidos (listado y detalle)
 *   CLIENTE  -> solo la tienda: comprar y pagar sus propios pedidos
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String API = ApiRutas.V1;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService) throws Exception {

        // Se crea aquí (y no como @Bean) para que solo lo use Spring Security
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService);

        http
            // Con JWT en el encabezado no se usan cookies de sesión,
            // por lo que la protección CSRF no aplica.
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())

            // No se crean sesiones HTTP: el token es la "credencial"
            .sessionManagement(sesion -> sesion
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .authorizeHttpRequests(auth -> auth

                // Documentación Swagger / OpenAPI y página de error
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/error"
                ).permitAll()

                // Login y logout (públicos: el logout solo confirma el cierre)
                .requestMatchers(
                    HttpMethod.POST,
                    API + "/auth/refresh",
                    API + "/auth/google",
                    API + "/admin/login",
                    API + "/admin/logout",
                    API + "/usuarios/login",
                    API + "/usuarios/logout",
                    API + "/usuarios/registro"
                ).permitAll()


                // Pedidos: el ADMIN y el VENDEDOR pueden ver el listado y el detalle
                .requestMatchers(HttpMethod.GET, API + "/pedidos/**").hasAnyRole("ADMIN", "VENDEDOR")

                // Consultas privadas del administrador
                .requestMatchers(API + "/usuarios/total-clientes").hasRole("ADMIN")

                // Rutas administrativas (perfil, gestión de clientes)
                .requestMatchers(
                    API + "/admin/**"
                ).hasRole("ADMIN")

                // Gestión de productos (incluye subir imágenes)
                .requestMatchers(HttpMethod.POST, API + "/productos/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, API + "/productos/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, API + "/productos/**").hasRole("ADMIN")

                // Gestión de categorías
                .requestMatchers(HttpMethod.POST, API + "/categorias/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, API + "/categorias/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, API + "/categorias/**").hasRole("ADMIN")

                // Compras: solo clientes autenticados
                .requestMatchers(HttpMethod.POST, API + "/pedidos").hasRole("CLIENTE")
                .requestMatchers(HttpMethod.POST, API + "/pedidos/*/pago").hasRole("CLIENTE")

                // Catálogo público (GET de productos, categorías e imágenes)
                .requestMatchers(HttpMethod.GET, API + "/**").permitAll()

                // Cualquier otra ruta de la API requiere iniciar sesión
                .requestMatchers(API + "/**").authenticated()

                .anyRequest().denyAll()
            )

            // El filtro JWT se ejecuta antes del filtro estándar de usuario/contraseña
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)

            .exceptionHandling(ex -> ex
                // 401: no hay token, o es inválido / está vencido
                .authenticationEntryPoint((request, response, error) -> {
                    boolean tokenInvalido =
                            request.getAttribute(JwtAuthenticationFilter.ATRIBUTO_ERROR) != null;
                    escribirError(response, HttpServletResponse.SC_UNAUTHORIZED,
                            tokenInvalido
                                    ? "Tu sesión expiró o el token no es válido. Inicia sesión nuevamente."
                                    : "Debes iniciar sesión para acceder a este recurso.",
                            request.getRequestURI());
                })
                // 403: hay token válido, pero el rol no tiene permiso
                .accessDeniedHandler((request, response, error) ->
                    escribirError(response, HttpServletResponse.SC_FORBIDDEN,
                            "No tienes permisos para realizar esta acción.",
                            request.getRequestURI())
                )
            );

        return http.build();
    }

    // CORS: permite que el frontend (otro puerto) llame a la API con el token
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.origenes:http://localhost:5173}") String origenes) {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origenes.split(","))
                .map(String::trim)
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(API + "/**", config);
        return source;
    }

    // Mismo formato JSON que GlobalExceptionHandler (status, error, ruta, fecha)
    private static void escribirError(HttpServletResponse response, int status,
                                      String mensaje, String ruta) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"status\":" + status
                        + ",\"error\":\"" + mensaje + "\""
                        + ",\"ruta\":\"" + ruta.replace("\"", "") + "\""
                        + ",\"fecha\":\"" + LocalDateTime.now() + "\"}");
    }
}
