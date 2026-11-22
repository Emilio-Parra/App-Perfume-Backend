
package com.tienda.perfumes.controller;

import com.tienda.perfumes.config.ApiRutas;
import com.tienda.perfumes.dto.LoginRequest;
import com.tienda.perfumes.dto.RegistroRequest;
import com.tienda.perfumes.model.Usuario;
import com.tienda.perfumes.security.JwtService;
import com.tienda.perfumes.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@Tag(name = "Usuarios", description = "Registro, login (JWT) y logout de clientes")
@RequestMapping(ApiRutas.V1 + "/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    public UsuarioController(UsuarioService usuarioService, JwtService jwtService) {
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
    }

    // Registrar un nuevo cliente
    @Operation(summary = "Registrar un nuevo cliente")
    @PostMapping("/registro")
    public ResponseEntity<Usuario> registrar(
            @Valid @RequestBody RegistroRequest datos) {

        // Las reglas (RUN, correo, largos) se validan con @Valid
        // y los duplicados en UsuarioService (409 Conflict).
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(usuarioService.registrar(datos));
    }

    // Iniciar sesión como cliente: devuelve sus datos + el token JWT
    @Operation(summary = "Iniciar sesión como cliente (devuelve un token JWT)")
    @PostMapping("/login")
    public ResponseEntity<?> iniciarSesion(@Valid @RequestBody LoginRequest datos) {

        Usuario usuario = usuarioService.autenticar(
                datos.correo(), datos.pass()
        ).orElse(null);

        if (usuario == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Correo o contraseña incorrectos."));
        }

        // El "sub" del token es el ID de MySQL: PedidoController lo usa
        // para saber qué cliente está comprando.
        String rol = usuario.getRol() == null
                ? UsuarioService.ROL_CLIENTE
                : usuario.getRol();

        String token = jwtService.generarToken(
                usuario.getId().toString(),
                rol.toUpperCase(Locale.ROOT),
                Map.of("correo", usuario.getCorreo())
        );

        String refreshToken = jwtService.generarRefreshToken(
        usuario.getId().toString(),
        rol.toUpperCase(Locale.ROOT)
        );

        // Mismos campos que React ya usaba, más el token
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

    // Consultar cantidad de clientes registrados (solo ADMIN, ver SecurityConfig)
    @Operation(summary = "Contar clientes registrados")
    @GetMapping("/total-clientes")
    public Map<String, Long> obtenerTotalClientes() {
        return Map.of("totalClientes", usuarioService.contarClientes());
    }

    // Con JWT el servidor no guarda sesiones: cerrar sesión = el navegador
    // borra su token. Este endpoint se mantiene para confirmar el cierre.
    @Operation(summary = "Cerrar la sesión del cliente (el frontend descarta el token)")
    @PostMapping("/logout")
    public Map<String, String> cerrarSesion() {
        return Map.of("mensaje", "Sesión de cliente cerrada correctamente.");
    }
}
