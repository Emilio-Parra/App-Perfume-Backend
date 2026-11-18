
package com.tienda.perfumes.service;

import com.tienda.perfumes.dto.CategoriaRequest;
import com.tienda.perfumes.exception.ConflictoException;
import com.tienda.perfumes.exception.RecursoNoEncontradoException;
import com.tienda.perfumes.model.Categoria;
import com.tienda.perfumes.model.Producto;
import com.tienda.perfumes.repository.CategoriaRepository;
import com.tienda.perfumes.repository.ProductoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    public CategoriaService(CategoriaRepository categoriaRepository,
                            ProductoRepository productoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
    }

    // Obtener todas las categorias
    @Transactional(readOnly = true)
    public List<Categoria> obtenerTodas() {
        return categoriaRepository.findAll();
    }

    // Buscar una categoria por su ID (puede no existir)
    @Transactional(readOnly = true)
    public Optional<Categoria> obtenerPorId(Long id) {
        return categoriaRepository.findById(id);
    }

    // Buscar una categoria por su ID o responder 404
    @Transactional(readOnly = true)
    public Categoria buscarPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la categoría con ID " + id + "."));
    }

    // Crear una nueva categoria (sin nombres repetidos)
    @Transactional
    public Categoria crear(CategoriaRequest datos) {
        String nombre = datos.nombre().trim();

        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoException("Ya existe una categoría llamada \"" + nombre + "\".");
        }

        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setDescripcion(limpiar(datos.descripcion()));
        return categoriaRepository.save(categoria);
    }

    // Actualizar una categoria existente
    @Transactional
    public Categoria actualizar(Long id, CategoriaRequest datos) {
        Categoria categoria = buscarPorId(id);
        String nombreAnterior = categoria.getNombre();
        String nombreNuevo = datos.nombre().trim();

        boolean cambiaNombre = !nombreNuevo.equalsIgnoreCase(nombreAnterior);
        if (cambiaNombre && categoriaRepository.existsByNombreIgnoreCase(nombreNuevo)) {
            throw new ConflictoException("Ya existe una categoría llamada \"" + nombreNuevo + "\".");
        }

        categoria.setNombre(nombreNuevo);
        categoria.setDescripcion(limpiar(datos.descripcion()));

        // Mantener asociados los productos si se renombra la categoría
        if (!nombreNuevo.equals(nombreAnterior)) {
            List<Producto> productos = productoRepository.findByTipo(nombreAnterior);
            productos.forEach(p -> p.setTipo(nombreNuevo));
            productoRepository.saveAll(productos);
        }

        return categoriaRepository.save(categoria);
    }

    // Eliminar una categoria (solo si no tiene productos)
    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscarPorId(id);

        long productos = productoRepository.countByTipo(categoria.getNombre());
        if (productos > 0) {
            throw new ConflictoException("No se puede eliminar \"" + categoria.getNombre()
                    + "\": tiene " + productos + " producto(s) asociado(s).");
        }

        categoriaRepository.delete(categoria);
    }

    private String limpiar(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
