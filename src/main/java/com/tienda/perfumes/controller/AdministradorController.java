package com.tienda.perfumes.controller;

import com.tienda.perfumes.config.ApiRutas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.tienda.perfumes.dto.LoginRequest;
import com.tienda.perfumes.model.Administrador;
import com.tienda.perfumes.security.JwtService;
import com.tienda.perfumes.service.AdministradorService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@Tag(name = "Administrador", description = "Login (JWT) y perfil del administrador")
@RequestMapping(ApiRutas.V1 + "/admin")
public class AdministradorController {

    private final AdministradorService administradorService;
    private final JwtService jwtService;

    public AdministradorController(
            AdministradorService administradorService,
            JwtService jwtService) {

        this.administradorService = administradorService;
        this.jwtService = jwtService;
    }

    private Map<String, Object> datosPublicos(Administrador admin) {
        return Map.of(
                "id", admin.getId(),
                "correo", admin.getCorreo(),
                "nombre", admin.getNombre() == null ? "Administrador" : admin.getNombre(),
                "apellido", admin.getApellido() == null ? "Perfumes Store" : admin.getApellido(),
                "rol", "admin"
        );
    }

    // Iniciar sesión como administrador: devuelve sus datos + el token JWT
    @Operation(summary = "Iniciar sesión como administrador (devuelve un token JWT)")
    @PostMapping("/login")
    public ResponseEntity<?> iniciarSesion(@Valid @RequestBody LoginRequest datos) {

        Administrador admin = administradorService.autenticar(
                datos.correo(),
                datos.pass()
        ).orElse(null);

        if (admin == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error", "Correo o contraseña incorrectos."
                    ));
        }

        // El "sub" del token del admin es su correo: /perfil lo usa para buscarlo
        String token = jwtService.generarToken(admin.getCorreo(), "ADMIN", Map.of());

        Map<String, Object> respuesta = new LinkedHashMap<>(datosPublicos(admin));
        respuesta.put("token", token);
        respuesta.put("tipoToken", "Bearer");
        respuesta.put("expiraEnMinutos", jwtService.getExpiracionMinutos());

        return ResponseEntity.ok(respuesta);
    }

    // Perfil guardado en MySQL; no se toman IDs o correos desde React.
    @Operation(summary = "Consultar el perfil del administrador autenticado")
    @GetMapping("/perfil")
    public ResponseEntity<?> consultarPerfil(Authentication autenticacion) {
        if (autenticacion == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return administradorService.obtenerPorCorreo(autenticacion.getName())
                .<ResponseEntity<?>>map(admin -> ResponseEntity.ok(datosPublicos(admin)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Administrador no encontrado.")));
    }

    public record CambiosPerfil(String nombre, String apellido, String pass) {}

    // Cambiar datos del administrador que está autenticado.
    @Operation(summary = "Actualizar nombre, apellido o contraseña del administrador")
    @PutMapping("/perfil")
    public ResponseEntity<?> actualizarPerfil(
            @RequestBody CambiosPerfil cambios,
            Authentication autenticacion) {

        if (autenticacion == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Los errores (400 datos inválidos, 404 no existe) los responde GlobalExceptionHandler
        Administrador actualizado = administradorService.actualizarPerfil(
                autenticacion.getName(),
                cambios.nombre(),
                cambios.apellido(),
                cambios.pass()
        );
        return ResponseEntity.ok(datosPublicos(actualizado));
    }

    // Con JWT el servidor no guarda sesiones: el frontend descarta el token
    @Operation(summary = "Cerrar la sesión del administrador (el frontend descarta el token)")
    @PostMapping("/logout")
    public Map<String, String> cerrarSesion() {
        return Map.of("mensaje", "Sesión cerrada correctamente.");
    }
}
