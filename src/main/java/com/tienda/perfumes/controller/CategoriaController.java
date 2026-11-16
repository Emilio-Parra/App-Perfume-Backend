
package com.tienda.perfumes.controller;

import com.tienda.perfumes.config.ApiRutas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.tienda.perfumes.model.Categoria;
import com.tienda.perfumes.service.CategoriaService;

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

    // Obtener una categoria por ID
    @Operation(summary = "Obtener una categoría por ID")
    @GetMapping("/{id}")
    public ResponseEntity<Categoria> obtenerPorId(@PathVariable Long id) {
        return categoriaService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Crear una nueva categoria
    @Operation(summary = "Crear una categoría")
    @PostMapping
    public ResponseEntity<Categoria> crearCategoria(@RequestBody Categoria categoria) {
        Categoria nueva = categoriaService.guardarCategoria(categoria);
        return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
    }

    // Actualizar una categoria
    @Operation(summary = "Actualizar una categoría")
    @PutMapping("/{id}")
    public ResponseEntity<Categoria> actualizarCategoria(
            @PathVariable Long id,
            @RequestBody Categoria categoria) {

        return categoriaService.actualizarCategoria(id, categoria)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Eliminar una categoria
    @Operation(summary = "Eliminar una categoría")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCategoria(@PathVariable Long id) {
        if (categoriaService.eliminarCategoria(id)) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.notFound().build();
    }
}
