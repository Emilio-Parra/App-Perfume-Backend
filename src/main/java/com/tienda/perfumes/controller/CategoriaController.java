
package com.tienda.perfumes.controller;

import com.tienda.perfumes.config.ApiRutas;
import com.tienda.perfumes.dto.CategoriaRequest;
import com.tienda.perfumes.model.Categoria;
import com.tienda.perfumes.service.CategoriaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Categorías", description = "Categorías de perfumes (CRUD)")
@RequestMapping(ApiRutas.V1 + "/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    // Obtener todas las categorias
    @Operation(summary = "Listar todas las categorías")
    @GetMapping
    public List<Categoria> obtenerTodas() {
        return categoriaService.obtenerTodas();
    }

    // Obtener una categoria por ID (404 si no existe)
    @Operation(summary = "Obtener una categoría por ID")
    @GetMapping("/{id}")
    public Categoria obtenerPorId(@PathVariable Long id) {
        return categoriaService.buscarPorId(id);
    }

    // Crear una nueva categoria (409 si el nombre ya existe)
    @Operation(summary = "Crear una categoría")
    @PostMapping
    public ResponseEntity<Categoria> crearCategoria(@Valid @RequestBody CategoriaRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.crear(datos));
    }

    // Actualizar una categoria
    @Operation(summary = "Actualizar una categoría")
    @PutMapping("/{id}")
    public Categoria actualizarCategoria(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaRequest datos) {

        return categoriaService.actualizar(id, datos);
    }

    // Eliminar una categoria (409 si tiene productos)
    @Operation(summary = "Eliminar una categoría")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCategoria(@PathVariable Long id) {
        categoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
