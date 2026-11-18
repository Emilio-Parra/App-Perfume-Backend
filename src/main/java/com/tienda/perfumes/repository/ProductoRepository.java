
package com.tienda.perfumes.repository;

import com.tienda.perfumes.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // Productos de una categoría
    List<Producto> findByTipo(String tipo);

    // Cantidad de productos de una categoría
    long countByTipo(String tipo);
}
