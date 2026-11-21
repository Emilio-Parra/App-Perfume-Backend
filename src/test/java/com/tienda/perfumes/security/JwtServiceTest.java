package com.tienda.perfumes.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;
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
    void rechazaTokenVencido() throws InterruptedException {
        JwtService tokensDeCeroMinutos = new JwtService(SECRETO, 0);
        String token = tokensDeCeroMinutos.generarToken("1", "ADMIN", Map.of());

        Thread.sleep(1100); // la fecha de vencimiento ya pasó

        assertThatThrownBy(() -> tokensDeCeroMinutos.validar(token))
                .isInstanceOf(ExpiredJwtException.class);
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
