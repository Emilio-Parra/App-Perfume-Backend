
package com.tienda.perfumes.repository;

import com.tienda.perfumes.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    // Comprobar si ya existe una categoria con ese nombre (sin importar mayúsculas)
    boolean existsByNombreIgnoreCase(String nombre);
}
