package com.tienda.perfumes.controller;

import com.tienda.perfumes.config.ApiRutas;
import com.tienda.perfumes.model.Usuario;
import com.tienda.perfumes.repository.UsuarioRepository;
import com.tienda.perfumes.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Login con Google Identity Services para cuentas existentes de la tienda.
 * La identidad se verifica con las claves publicas de Google (RS256).
 * NO da permisos ADMIN, NO acepta correos enviados por el frontend sin verificar,
 * NO crea usuarios incompletos sin RUN/direccion, y conserva los JWT propios.
 */
@RestController
@RequestMapping(ApiRutas.V1 + "/auth")
@Tag(name = "Autenticación", description = "Renovación JWT e inicio con Google")
public class GoogleAuthController {

    private static final String GOOGLE_JWKS = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> ROLES_VALIDOS = Set.of("cliente", "vendedor");

    private final UsuarioRepository usuarios;
    private final JwtService jwtService;
    private final NimbusJwtDecoder googleDecoder;
    private final String googleClientId;

    public GoogleAuthController(UsuarioRepository usuarios, JwtService jwtService,
            @Value("${GOOGLE_CLIENT_ID:}") String googleClientId) {
        this.usuarios = usuarios;
        this.jwtService = jwtService;
        this.googleClientId = googleClientId == null ? "" : googleClientId.trim();
        this.googleDecoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWKS)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
    }

    public record GoogleRequest(
            @NotBlank(message = "La credencial de Google es obligatoria.") String credential) {}

    @PostMapping("/google")
    @Transactional
    @Operation(summary = "Iniciar sesión con Google (cuenta cliente/vendedor ya registrada)")
    public ResponseEntity<?> iniciarConGoogle(@Valid @RequestBody GoogleRequest solicitud) {
        if (googleClientId.isBlank()) {
            return error(HttpStatus.SERVICE_UNAVAILABLE,
                    "El inicio con Google todavía no está configurado en este servidor.");
        }

        Jwt jwt;
        try {
            // Verifica firma y fechas de expiracion con las claves publicas de Google.
            jwt = googleDecoder.decode(solicitud.credential());
        } catch (JwtException | IllegalArgumentException ex) {
            return error(HttpStatus.UNAUTHORIZED, "La credencial de Google no es válida.");
        }

        String issuer = jwt.getIssuer() == null ? "" : jwt.getIssuer().toString();
        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String dominioAlojado = jwt.getClaimAsString("hd");
        Object verificado = jwt.getClaim("email_verified");
        boolean emailVerificado = Boolean.TRUE.equals(verificado)
                || "true".equalsIgnoreCase(String.valueOf(verificado));

        if (!("https://accounts.google.com".equals(issuer)
                || "accounts.google.com".equals(issuer))
                || jwt.getAudience() == null || !jwt.getAudience().contains(googleClientId)
                || jwt.getExpiresAt() == null
                || subject == null || subject.isBlank()
                || !emailVerificado || email == null || email.isBlank()) {
            return error(HttpStatus.UNAUTHORIZED, "La cuenta de Google no se pudo verificar.");
        }

        // Google es autoridad sobre @gmail.com y dominios de Google Workspace
        // presentes en el claim 'hd'. Para otros correos, no vincular por email.
        String correo = email.trim().toLowerCase(Locale.ROOT);
        int arroba = correo.lastIndexOf('@');
        if (arroba < 1) {
            return error(HttpStatus.UNAUTHORIZED, "El correo de Google no es válido.");
        }
        String dominio = correo.substring(arroba + 1);
        boolean correoControladoPorGoogle = "gmail.com".equals(dominio)
                || (dominioAlojado != null
                    && dominio.equals(dominioAlojado.toLowerCase(Locale.ROOT)));
        if (!correoControladoPorGoogle) {
            return error(HttpStatus.FORBIDDEN,
                    "Usa una cuenta Gmail o una cuenta institucional de Google Workspace.");
        }

        // 'sub' es un identificador estable de Google; el email puede cambiar.
        Usuario usuario = usuarios.findByGoogleSub(subject).orElse(null);
        if (usuario == null) {
            // Vinculacion inicial solo con correo verificado por Google.
            usuario = usuarios.findByCorreo(correo).orElse(null);
            if (usuario == null) {
                return error(HttpStatus.CONFLICT,
                        "Primero regístrate en la tienda con ese correo y después usa Google.");
            }
            if (usuario.getGoogleSub() != null && !usuario.getGoogleSub().equals(subject)) {
                return error(HttpStatus.FORBIDDEN, "Esta cuenta ya está vinculada a otro usuario de Google.");
            }
        }

        String rol = usuario.getRol() == null ? "" : usuario.getRol().toLowerCase(Locale.ROOT);
        if (!ROLES_VALIDOS.contains(rol)) {
            return error(HttpStatus.FORBIDDEN,
                    "Este tipo de cuenta debe iniciar sesión por el acceso habitual.");
        }

        if (usuario.getGoogleSub() == null) {
            usuario.setGoogleSub(subject);
            usuarios.save(usuario);
        }

        // Mismo formato de respuesta que /usuarios/login: React, roles, JWT y refresh
        // siguen funcionando sin ningun cambio en carrito, compras o panel vendedor.
        String token = jwtService.generarToken(usuario.getId().toString(),
                rol.toUpperCase(Locale.ROOT), Map.of("correo", usuario.getCorreo()));
        String refreshToken = jwtService.generarRefreshToken(usuario.getId().toString(),
                rol.toUpperCase(Locale.ROOT));

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("id", usuario.getId());
        respuesta.put("rut", usuario.getRut());
        respuesta.put("correo", usuario.getCorreo());
        respuesta.put("nombre", usuario.getNombre());
        respuesta.put("apellido", usuario.getApellido());
        respuesta.put("rol", rol);
        respuesta.put("fechaNac", usuario.getFechaNac());
        respuesta.put("region", usuario.getRegion());
        respuesta.put("comuna", usuario.getComuna());
        respuesta.put("direccion", usuario.getDireccion());
        respuesta.put("token", token);
        respuesta.put("refreshToken", refreshToken);
        respuesta.put("tipoToken", "Bearer");
        respuesta.put("expiraEnMinutos", jwtService.getExpiracionMinutos());
        return ResponseEntity.ok(respuesta);
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("error", mensaje));
    }
}
