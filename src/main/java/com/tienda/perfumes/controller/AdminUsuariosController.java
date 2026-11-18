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

// Esta API esta protegida por /api/v1/admin/** en SecurityConfig.
// Los administradores se autentican con su propia tabla: aqui solo se gestionan clientes.
// Toda la lógica (duplicados, contraseñas, pedidos asociados) está en UsuarioService.
@RestController
@Tag(name = "Admin - Clientes", description = "Gestión de clientes desde el panel")
@RequestMapping(ApiRutas.V1 + "/admin/usuarios")
public class AdminUsuariosController {

    private final UsuarioService usuarioService;

    public AdminUsuariosController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Listar clientes")
    @GetMapping
    public List<Usuario> listar() {
        return usuarioService.listarClientes();
    }

    @Operation(summary = "Crear un cliente")
    @PostMapping
    public ResponseEntity<Usuario> crear(@Valid @RequestBody UsuarioAdminRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crearCliente(datos));
    }

    @Operation(summary = "Editar un cliente por correo")
    @PutMapping("/{correo}")
    public Usuario editar(@PathVariable String correo,
                          @Valid @RequestBody UsuarioAdminRequest datos) {
        return usuarioService.actualizarCliente(correo, datos);
    }

    @Operation(summary = "Eliminar un cliente (si no tiene pedidos)")
    @DeleteMapping("/{correo}")
    public ResponseEntity<Void> eliminar(@PathVariable String correo) {
        usuarioService.eliminarCliente(correo);
        return ResponseEntity.noContent().build();
    }
}
