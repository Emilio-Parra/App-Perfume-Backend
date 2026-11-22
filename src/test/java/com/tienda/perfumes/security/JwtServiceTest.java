package com.tienda.perfumes.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService: generación y validación de tokens")
class JwtServiceTest {

    private static final String SECRETO = "clave-de-prueba-con-mas-de-32-caracteres-123";

    private final JwtService jwtService = new JwtService(SECRETO, 60);

    @Test
    @DisplayName("Genera un token con el usuario, el rol y los datos extra")
    void generaYValidaToken() {
        String token = jwtService.generarToken("15", "CLIENTE", Map.of("correo", "ana@duoc.cl"));

        Claims datos = jwtService.validar(token);

        assertThat(token.split("\\.")).hasSize(3); // header.payload.firma
        assertThat(datos.getSubject()).isEqualTo("15");
        assertThat(datos.get(JwtService.CLAIM_ROL, String.class)).isEqualTo("CLIENTE");
        assertThat(datos.get("correo", String.class)).isEqualTo("ana@duoc.cl");
        assertThat(datos.getIssuer()).isEqualTo("perfumes-store");
        assertThat(datos.getExpiration()).isAfter(datos.getIssuedAt());
    }

    @Test
    @DisplayName("Rechaza un token cuyo rol fue modificado (la firma no coincide)")
    void rechazaTokenModificado() {
        String token = jwtService.generarToken("15", "CLIENTE", Map.of());
        String[] partes = token.split("\\.");

        // Un cliente intenta cambiarse el rol a ADMIN dentro del payload
        String payload = new String(Base64.getUrlDecoder().decode(partes[1]));
        String payloadFalso = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.replace("CLIENTE", "ADMIN").getBytes());
        String tokenFalso = partes[0] + "." + payloadFalso + "." + partes[2];

        assertThatThrownBy(() -> jwtService.validar(tokenFalso))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("Rechaza un token firmado con otra clave")
    void rechazaTokenDeOtraClave() {
        JwtService otroServidor = new JwtService("otra-clave-distinta-de-mas-de-32-caracteres!!", 60);
        String tokenAjeno = otroServidor.generarToken("1", "ADMIN", Map.of());

        assertThatThrownBy(() -> jwtService.validar(tokenAjeno))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Rechaza un token vencido")
    void rechazaTokenVencido() {
        // El servicio exige una duracion positiva; fabricamos un JWT ya vencido
        // usando la misma clave y los claims obligatorios del token de acceso.
        Instant ahora = Instant.now();
        String tokenVencido = Jwts.builder()
                .subject("1")
                .claim(JwtService.CLAIM_ROL, "ADMIN")
                .claim(JwtService.CLAIM_TIPO, JwtService.TIPO_ACCESO)
                .issuer("perfumes-store")
                .issuedAt(Date.from(ahora.minusSeconds(180)))
                .expiration(Date.from(ahora.minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwtService.validar(tokenVencido))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("El refresh token se valida para renovacion, pero no sirve para entrar a la API")
    void refreshTokenNoEsTokenDeAcceso() {
        String refresh = jwtService.generarRefreshToken("15", "CLIENTE");

        assertThat(jwtService.validarRefresh(refresh).getSubject()).isEqualTo("15");
        assertThatThrownBy(() -> jwtService.validar(refresh))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("El token de acceso no sirve como refresh token")
    void accesoNoEsRefreshToken() {
        String token = jwtService.generarToken("15", "CLIENTE", Map.of());

        assertThatThrownBy(() -> jwtService.validarRefresh(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Rechaza texto que no es un JWT")
    void rechazaTextoCualquiera() {
        assertThatThrownBy(() -> jwtService.validar("hola"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("No permite una clave secreta de menos de 32 caracteres")
    void exigeClaveSegura() {
        assertThatThrownBy(() -> new JwtService("corta", 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }
}
