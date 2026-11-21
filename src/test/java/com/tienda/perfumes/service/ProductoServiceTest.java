package com.tienda.perfumes.service;

import com.tienda.perfumes.dto.ProductoRequest;
import com.tienda.perfumes.exception.RecursoNoEncontradoException;
import com.tienda.perfumes.model.Producto;
import com.tienda.perfumes.repository.ProductoRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductoService: CRUD de perfumes")
class ProductoServiceTest {

    @Mock private ProductoRepository productoRepository;

    @InjectMocks private ProductoService productoService;

    private ProductoRequest datos(String genero, Integer descuento) {
        return new ProductoRequest(" Lattafa ", " Eclaire ", "arabe", genero, 29990,
                descuento, 10, "100 ml", "/img/a.jpg", "Fragancia gourmand dulce y cremosa.");
    }

    @Test
    @DisplayName("Crea el producto limpiando textos y con descuento 0 si no se indica")
    void creaProducto() {
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        Producto creado = productoService.crear(datos("MUJER", null));

        assertThat(creado.getMarca()).isEqualTo("Lattafa");
        assertThat(creado.getModelo()).isEqualTo("Eclaire");
        assertThat(creado.getGenero()).isEqualTo("mujer");
        assertThat(creado.getDescuento()).isZero();
        assertThat(creado.getId()).isNull(); // el ID lo genera MySQL
    }

    @Test
    @DisplayName("Si no se indica género, queda como unisex")
    void generoPorDefecto() {
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(productoService.crear(datos(null, 5)).getGenero()).isEqualTo("unisex");
    }

    @Test
    @DisplayName("Actualiza un producto existente conservando su ID")
    void actualizaProducto() {
        Producto existente = new Producto();
        existente.setId(4L);
        when(productoRepository.findById(4L)).thenReturn(Optional.of(existente));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        Producto actualizado = productoService.actualizar(4L, datos("hombre", 15));

        assertThat(actualizado.getId()).isEqualTo(4L);
        assertThat(actualizado.getDescuento()).isEqualTo(15);
    }

    @Test
    @DisplayName("Actualizar o eliminar un producto inexistente responde 404")
    void productoInexistente() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.actualizar(99L, datos("hombre", 0)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");
        assertThatThrownBy(() -> productoService.eliminar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(productoRepository, never()).save(any());
        verify(productoRepository, never()).delete(any());
    }
}
