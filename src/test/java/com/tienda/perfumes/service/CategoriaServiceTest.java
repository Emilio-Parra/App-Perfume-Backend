package com.tienda.perfumes.service;

import com.tienda.perfumes.dto.CategoriaRequest;
import com.tienda.perfumes.exception.ConflictoException;
import com.tienda.perfumes.exception.RecursoNoEncontradoException;
import com.tienda.perfumes.model.Categoria;
import com.tienda.perfumes.model.Producto;
import com.tienda.perfumes.repository.CategoriaRepository;
import com.tienda.perfumes.repository.ProductoRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoriaService: reglas de las categorías")
class CategoriaServiceTest {

    @Mock private CategoriaRepository categoriaRepository;
    @Mock private ProductoRepository productoRepository;

    @InjectMocks private CategoriaService categoriaService;

    private Categoria categoria(Long id, String nombre) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setNombre(nombre);
        return c;
    }

    @Test
    @DisplayName("Crea una categoría quitando espacios sobrantes")
    void creaCategoria() {
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        Categoria nueva = categoriaService.crear(new CategoriaRequest("  nicho  ", null));

        assertThat(nueva.getNombre()).isEqualTo("nicho");
        assertThat(nueva.getDescripcion()).isEmpty();
    }

    @Test
    @DisplayName("No permite nombres repetidos, sin importar mayúsculas (409)")
    void rechazaNombreRepetido() {
        when(categoriaRepository.existsByNombreIgnoreCase("Arabe")).thenReturn(true);

        assertThatThrownBy(() -> categoriaService.crear(new CategoriaRequest("Arabe", "")))
                .isInstanceOf(ConflictoException.class);

        verify(categoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("No elimina una categoría que tiene productos (409)")
    void noEliminaConProductos() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria(1L, "arabe")));
        when(productoRepository.countByTipo("arabe")).thenReturn(9L);

        assertThatThrownBy(() -> categoriaService.eliminar(1L))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("9 producto(s)");

        verify(categoriaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Elimina una categoría vacía")
    void eliminaCategoriaVacia() {
        Categoria vacia = categoria(2L, "nueva");
        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(vacia));
        when(productoRepository.countByTipo("nueva")).thenReturn(0L);

        categoriaService.eliminar(2L);

        verify(categoriaRepository).delete(vacia);
    }

    @Test
    @DisplayName("Al renombrar, los productos de la categoría se actualizan")
    void renombrarActualizaProductos() {
        Producto p = new Producto();
        p.setTipo("arabe");
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria(1L, "arabe")));
        when(categoriaRepository.existsByNombreIgnoreCase("árabe")).thenReturn(false);
        when(productoRepository.findByTipo("arabe")).thenReturn(List.of(p));
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        categoriaService.actualizar(1L, new CategoriaRequest("árabe", "Fragancias árabes"));

        assertThat(p.getTipo()).isEqualTo("árabe");
        verify(productoRepository).saveAll(List.of(p));
    }

    @Test
    @DisplayName("Responde 404 si la categoría no existe")
    void noExiste() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaService.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
