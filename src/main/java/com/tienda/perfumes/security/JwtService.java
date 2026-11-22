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

/** Genera y valida JWT de acceso y de renovacion para Perfumes Store. */
@Service
public class JwtService {

    public static final String CLAIM_ROL = "rol";
    public static final String CLAIM_TIPO = "tipo";

    public static final String TIPO_ACCESO = "acceso";
    public static final String TIPO_REFRESH = "refresh";

    private static final String EMISOR = "perfumes-store";

    private final SecretKey clave;
    private final Duration duracion;
    private final Duration duracionRefresh = Duration.ofDays(7);

    public JwtService(
            @Value("${app.jwt.secret}") String secreto,
            @Value("${app.jwt.expiracion-minutos:120}") long minutos) {

        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret debe tener al menos 32 caracteres (256 bits).");
        }
        if (minutos <= 0) {
            throw new IllegalStateException("app.jwt.expiracion-minutos debe ser positivo.");
        }

        this.clave = Keys.hmacShaKeyFor(bytes);
        this.duracion = Duration.ofMinutes(minutos);
    }

    /** Token que puede utilizarse para llamar a los endpoints protegidos. */
    public String generarToken(String sujeto, String rol, Map<String, Object> extras) {
        return generar(sujeto, rol, extras, TIPO_ACCESO, duracion);
    }

    /** Token de renovacion: no autoriza endpoints protegidos. */
    public String generarRefreshToken(String sujeto, String rol) {
        return generar(sujeto, rol, Map.of(), TIPO_REFRESH, duracionRefresh);
    }

    private String generar(String sujeto, String rol, Map<String, Object> extras,
                          String tipo, Duration tiempo) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .claims(extras)
                .subject(sujeto)
                .claim(CLAIM_ROL, rol)
                .claim(CLAIM_TIPO, tipo)
                .issuer(EMISOR)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(tiempo)))
                .signWith(clave)
                .compact();
    }

    /** El filtro de Spring Security solo debe aceptar JWT de acceso. */
    public Claims validar(String token) throws JwtException {
        return validarTipo(token, TIPO_ACCESO);
    }

    /** Para el futuro endpoint de renovacion: solo acepta refresh tokens. */
    public Claims validarRefresh(String token) throws JwtException {
        return validarTipo(token, TIPO_REFRESH);
    }

    private Claims validarTipo(String token, String tipoEsperado) throws JwtException {
        Claims datos = Jwts.parser()
                .verifyWith(clave)
                .requireIssuer(EMISOR)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String tipoReal = datos.get(CLAIM_TIPO, String.class);
        if (!tipoEsperado.equals(tipoReal)) {
            throw new JwtException("Tipo de token incorrecto.");
        }
        return datos;
    }

    public long getExpiracionMinutos() {
        return duracion.toMinutes();
    }

    public long getRefreshExpiracionMinutos() {
        return duracionRefresh.toMinutes();
    }
}
