package com.tienda.perfumes.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * Crea y valida los tokens JWT de la tienda.
 *
 * Un token tiene tres partes separadas por puntos: header.payload.firma
 *  - payload: quién es el usuario (sub), su rol y cuándo vence (exp)
 *  - firma:   HMAC-SHA256 con una clave secreta que solo conoce el backend.
 *             Si alguien modifica el token (por ejemplo, cambia su rol a ADMIN),
 *             la firma deja de coincidir y el token se rechaza.
 */
@Service
public class JwtService {

    public static final String CLAIM_ROL = "rol";
    private static final String EMISOR = "perfumes-store";

    private final SecretKey clave;
    private final Duration duracion;

    public JwtService(
            @Value("${app.jwt.secret}") String secreto,
            @Value("${app.jwt.expiracion-minutos:120}") long minutos) {

        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret debe tener al menos 32 caracteres (256 bits).");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.duracion = Duration.ofMinutes(minutos);
    }

    /**
     * Genera un token firmado.
     *
     * @param sujeto identificador del usuario (ID del cliente o correo del admin)
     * @param rol    ADMIN, VENDEDOR o CLIENTE
     * @param extras datos adicionales que se quieran incluir (ej. correo)
     */
    public String generarToken(String sujeto, String rol, Map<String, Object> extras) {
        Instant ahora = Instant.now();

        return Jwts.builder()
                .claims(extras)
                .subject(sujeto)
                .claim(CLAIM_ROL, rol)
                .issuer(EMISOR)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(duracion)))
                .signWith(clave)
                .compact();
    }

    /**
     * Comprueba la firma, el emisor y la fecha de vencimiento.
     * Si algo no cuadra lanza JwtException.
     */
    public Claims validar(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(clave)
                .requireIssuer(EMISOR)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpiracionMinutos() {
        return duracion.toMinutes();
    }
}
