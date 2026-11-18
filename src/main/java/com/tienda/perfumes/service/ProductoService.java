package com.tienda.perfumes.service;

import com.tienda.perfumes.dto.ProductoRequest;
import com.tienda.perfumes.exception.RecursoNoEncontradoException;
import com.tienda.perfumes.model.Producto;
import com.tienda.perfumes.repository.ProductoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    // Obtener todos los productos
    @Transactional(readOnly = true)
    public List<Producto> obtenerTodos() {
        return productoRepository.findAll();
    }

    // Buscar un producto por su ID (puede no existir)
    @Transactional(readOnly = true)
    public Optional<Producto> obtenerPorId(Long id) {
        return productoRepository.findById(id);
    }

    // Buscar un producto por su ID o responder 404
    @Transactional(readOnly = true)
    public Producto buscarPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el producto con ID " + id + "."));
    }

    // Crear un nuevo producto
    @Transactional
    public Producto crear(ProductoRequest datos) {
        Producto producto = new Producto();
        copiarDatos(datos, producto);
        return productoRepository.save(producto);
    }

    // Actualizar un producto existente
    @Transactional
    public Producto actualizar(Long id, ProductoRequest datos) {
        Producto producto = buscarPorId(id);
        copiarDatos(datos, producto);
        return productoRepository.save(producto);
    }

    // Eliminar un producto
    @Transactional
    public void eliminar(Long id) {
        Producto producto = buscarPorId(id);
        // Los pedidos guardan una copia de marca, modelo y precio,
        // por eso el historial de boletas no se pierde al borrar el producto.
        productoRepository.delete(producto);
    }

    // Pasar los datos validados del DTO a la entidad, normalizando textos
    private void copiarDatos(ProductoRequest datos, Producto producto) {
        producto.setMarca(datos.marca().trim());
        producto.setModelo(datos.modelo().trim());
        // Se respeta el nombre tal cual, porque el frontend compara p.tipo === categoria.nombre
        producto.setTipo(datos.tipo().trim());
        producto.setGenero(datos.genero() == null || datos.genero().isBlank()
                ? "unisex"
                : datos.genero().trim().toLowerCase(Locale.ROOT));
        producto.setPrecio(datos.precio());
        producto.setDescuento(datos.descuento() == null ? 0 : datos.descuento());
        producto.setStock(datos.stock());
        producto.setVolumen(datos.volumen().trim());
        producto.setImagenes(datos.imagenes().trim());
        producto.setDescripcion(datos.descripcion().trim());
    }
}
