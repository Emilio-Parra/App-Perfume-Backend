package com.tienda.perfumes.controller;

import com.tienda.perfumes.config.ApiRutas;
import com.tienda.perfumes.dto.UsuarioAdminRequest;
import com.tienda.perfumes.model.Usuario;
import com.tienda.perfumes.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Esta API esta protegida por /api/v1/admin/** en SecurityConfig (solo ADMIN).
// Los administradores se autentican con su propia tabla: aqui se gestionan
// clientes y vendedores. Toda la lógica está en UsuarioService.
@RestController
@Tag(name = "Admin - Usuarios", description = "Gestión de clientes y vendedores desde el panel (solo ADMIN)")
@RequestMapping(ApiRutas.V1 + "/admin/usuarios")
public class AdminUsuariosController {

    private final UsuarioService usuarioService;

    public AdminUsuariosController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Listar clientes y vendedores")
    @GetMapping
    public List<Usuario> listar() {
        return usuarioService.listarUsuariosGestionables();
    }

    @Operation(summary = "Crear un cliente o vendedor (campo rol: cliente | vendedor)")
    @PostMapping
    public ResponseEntity<Usuario> crear(@Valid @RequestBody UsuarioAdminRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crearUsuario(datos));
    }

    @Operation(summary = "Editar un cliente o vendedor por correo")
    @PutMapping("/{correo}")
    public Usuario editar(@PathVariable String correo,
                          @Valid @RequestBody UsuarioAdminRequest datos) {
        return usuarioService.actualizarUsuario(correo, datos);
    }

    @Operation(summary = "Eliminar un cliente o vendedor (si no tiene pedidos)")
    @DeleteMapping("/{correo}")
    public ResponseEntity<Void> eliminar(@PathVariable String correo) {
        usuarioService.eliminarUsuario(correo);
        return ResponseEntity.noContent().build();
    }
}
