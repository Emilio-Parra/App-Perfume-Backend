package com.tienda.perfumes.controller;

import com.tienda.perfumes.config.ApiRutas;
import com.tienda.perfumes.model.Usuario;
import com.tienda.perfumes.repository.AdministradorRepository;
import com.tienda.perfumes.repository.UsuarioRepository;
import com.tienda.perfumes.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(ApiRutas.V1 + "/auth")
@Tag(name = "Autenticación", description = "Renovación segura de tokens JWT")
public class AuthController {

    private final JwtService jwtService;
    private final AdministradorRepository administradores;
    private final UsuarioRepository usuarios;

    public AuthController(JwtService jwtService,
                          AdministradorRepository administradores,
                          UsuarioRepository usuarios) {
        this.jwtService = jwtService;
        this.administradores = administradores;
        this.usuarios = usuarios;
    }

    public record RefreshRequest(
            @NotBlank(message = "El refreshToken es obligatorio.") String refreshToken) {
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar un token de acceso usando un token de renovación vigente")
    public ResponseEntity<?> renovar(@Valid @RequestBody RefreshRequest solicitud) {
        Claims datos;
        try {
            datos = jwtService.validarRefresh(solicitud.refreshToken());
        } catch (JwtException | IllegalArgumentException ex) {
            return noAutorizado();
        }

        String sujeto = datos.getSubject();
        String rol = datos.get(JwtService.CLAIM_ROL, String.class);
        if (sujeto == null || sujeto.isBlank() || rol == null) {
            return noAutorizado();
        }

        String rolNormalizado = rol.toUpperCase(Locale.ROOT);
        Map<String, Object> extras;
        String sujetoActual;

        if ("ADMIN".equals(rolNormalizado)) {
            var admin = administradores.findByCorreo(sujeto.toLowerCase(Locale.ROOT));
            if (admin.isEmpty()) {
                return noAutorizado();
            }
            sujetoActual = admin.get().getCorreo();
            extras = Map.of();
        } else if ("CLIENTE".equals(rolNormalizado) || "VENDEDOR".equals(rolNormalizado)) {
            Long usuarioId;
            try {
                usuarioId = Long.valueOf(sujeto);
            } catch (NumberFormatException ex) {
                return noAutorizado();
            }

            Optional<Usuario> encontrado = usuarios.findById(usuarioId);
            if (encontrado.isEmpty()) {
                return noAutorizado();
            }
            Usuario usuario = encontrado.get();
            if (usuario.getRol() == null
                    || !rolNormalizado.equals(usuario.getRol().toUpperCase(Locale.ROOT))
                    || usuario.getCorreo() == null) {
                return noAutorizado();
            }

            sujetoActual = usuario.getId().toString();
            extras = Map.of("correo", usuario.getCorreo());
        } else {
            return noAutorizado();
        }

        String nuevoToken = jwtService.generarToken(sujetoActual, rolNormalizado, extras);
        return ResponseEntity.ok(Map.of(
                "token", nuevoToken,
                "tipoToken", "Bearer",
                "expiraEnMinutos", jwtService.getExpiracionMinutos()
        ));
    }

    private ResponseEntity<Map<String, String>> noAutorizado() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "El token de renovación no es válido o la cuenta ya no tiene acceso."));
    }
}
