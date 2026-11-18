package com.tienda.perfumes.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Se ejecuta una vez en cada petición, antes de los controladores.
 *
 * 1. Busca el encabezado  Authorization: Bearer <token>
 * 2. Valida el token con JwtService.
 * 3. Si es válido, registra al usuario y su rol (ROLE_ADMIN, ROLE_CLIENTE...)
 *    en Spring Security. Así funcionan hasRole(...) y el parámetro Authentication.
 *
 * Si no hay token o es inválido, la petición sigue como anónima y
 * SecurityConfig decide si la ruta lo permite (401 si no).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String ATRIBUTO_ERROR = "jwt_error";
    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String encabezado = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (encabezado != null && encabezado.startsWith(PREFIJO)) {
            String token = encabezado.substring(PREFIJO.length()).trim();

            try {
                Claims datos = jwtService.validar(token);
                String sujeto = datos.getSubject();
                String rol = datos.get(JwtService.CLAIM_ROL, String.class);

                if (sujeto != null && rol != null
                        && SecurityContextHolder.getContext().getAuthentication() == null) {

                    var autenticacion = new UsernamePasswordAuthenticationToken(
                            sujeto,
                            null,
                            List.of(new SimpleGrantedAuthority(
                                    "ROLE_" + rol.toUpperCase(Locale.ROOT)))
                    );
                    SecurityContextHolder.getContext().setAuthentication(autenticacion);
                }
            } catch (JwtException | IllegalArgumentException e) {
                // Token vencido, alterado o mal formado: se marca para dar un mensaje claro
                SecurityContextHolder.clearContext();
                request.setAttribute(ATRIBUTO_ERROR, true);
            }
        }

        filterChain.doFilter(request, response);
    }
}
