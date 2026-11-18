
package com.tienda.perfumes.controller;

import com.tienda.perfumes.config.ApiRutas;
import com.tienda.perfumes.dto.ProductoRequest;
import com.tienda.perfumes.model.Producto;
import com.tienda.perfumes.service.ProductoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Productos", description = "Catálogo de perfumes (CRUD)")
@RequestMapping(ApiRutas.V1 + "/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    // Obtener todos los productos
    @Operation(summary = "Listar todos los productos")
    @GetMapping
    public List<Producto> obtenerTodos() {
        return productoService.obtenerTodos();
    }

    // Obtener un producto por ID (404 si no existe)
    @Operation(summary = "Obtener un producto por ID")
    @GetMapping("/{id}")
    public Producto obtenerPorId(@PathVariable Long id) {
        return productoService.buscarPorId(id);
    }

    // Crear un nuevo producto (los datos se validan con @Valid)
    @Operation(summary = "Crear un producto")
    @PostMapping
    public ResponseEntity<Producto> crearProducto(@Valid @RequestBody ProductoRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(datos));
    }

    // Actualizar un producto
    @Operation(summary = "Actualizar un producto")
    @PutMapping("/{id}")
    public Producto actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody ProductoRequest datos) {

        return productoService.actualizar(id, datos);
    }

    // Eliminar un producto
    @Operation(summary = "Eliminar un producto")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
