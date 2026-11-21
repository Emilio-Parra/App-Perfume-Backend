package com.tienda.perfumes.service;

import com.tienda.perfumes.dto.CrearPedidoRequest;
import com.tienda.perfumes.dto.CrearPedidoRequest.ClienteRequest;
import com.tienda.perfumes.dto.CrearPedidoRequest.EnvioRequest;
import com.tienda.perfumes.dto.CrearPedidoRequest.ItemPedidoRequest;
import com.tienda.perfumes.exception.ConflictoException;
import com.tienda.perfumes.exception.RecursoNoEncontradoException;
import com.tienda.perfumes.model.DetallePedido;
import com.tienda.perfumes.model.Pedido;
import com.tienda.perfumes.model.Producto;
import com.tienda.perfumes.model.Usuario;
import com.tienda.perfumes.repository.PedidoRepository;
import com.tienda.perfumes.repository.ProductoRepository;
import com.tienda.perfumes.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoService: compras, totales, stock y pagos")
class PedidoServiceTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SimuladorPago simuladorPago;

    @InjectMocks private PedidoService pedidoService;

    private Usuario cliente;
    private Producto perfume;

    @BeforeEach
    void datosDePrueba() {
        cliente = new Usuario();
        cliente.setId(7L);
        cliente.setCorreo("ana@duoc.cl");

        perfume = new Producto();
        perfume.setId(1L);
        perfume.setMarca("Lattafa");
        perfume.setModelo("Eclaire");
        perfume.setTipo("arabe");
        perfume.setPrecio(30000);
        perfume.setDescuento(10);
        perfume.setStock(5);
    }

    private CrearPedidoRequest pedido(Long usuarioId, int cantidad) {
        return new CrearPedidoRequest(
                usuarioId,
                List.of(new ItemPedidoRequest(1L, cantidad)),
                new ClienteRequest("Ana", "Paz", "ana@duoc.cl"),
                new EnvioRequest("Calle 1", null, "RM", "Maipú", null));
    }

    @Nested
    @DisplayName("Crear pedido")
    class CrearPedido {

        @BeforeEach
        void guardarDevuelveElMismoPedido() {
            // Algunas pruebas no llegan a guardar: lenient evita avisos de Mockito
            org.mockito.Mockito.lenient()
                    .when(pedidoRepository.save(any(Pedido.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("Calcula el total con el precio de MySQL y el descuento (no el del navegador)")
        void calculaTotalConDescuento() {
            when(usuarioRepository.findById(7L)).thenReturn(Optional.of(cliente));
            when(productoRepository.findById(1L)).thenReturn(Optional.of(perfume));

            Pedido resultado = pedidoService.crearPedido(pedido(7L, 2));

            // 30.000 con 10% de descuento = 27.000 x 2 unidades
            assertThat(resultado.getTotal()).isEqualTo(54000);
            assertThat(resultado.getEstado()).isEqualTo("pendiente");
            assertThat(resultado.getCodigo()).startsWith("ORDER");
            assertThat(resultado.getDetalles()).hasSize(1);

            DetallePedido detalle = resultado.getDetalles().get(0);
            assertThat(detalle.getPrecio()).isEqualTo(27000);
            assertThat(detalle.getMarca()).isEqualTo("Lattafa"); // copia para el historial
        }

        @Test
        @DisplayName("Crear el pedido NO descuenta stock (se descuenta al pagar)")
        void crearNoDescuentaStock() {
            when(usuarioRepository.findById(7L)).thenReturn(Optional.of(cliente));
            when(productoRepository.findById(1L)).thenReturn(Optional.of(perfume));

            pedidoService.crearPedido(pedido(7L, 2));

            assertThat(perfume.getStock()).isEqualTo(5);
        }

        @Test
        @DisplayName("Rechaza comprar más unidades que el stock disponible (409)")
        void rechazaSinStock() {
            when(usuarioRepository.findById(7L)).thenReturn(Optional.of(cliente));
            when(productoRepository.findById(1L)).thenReturn(Optional.of(perfume));

            assertThatThrownBy(() -> pedidoService.crearPedido(pedido(7L, 6)))
                    .isInstanceOf(ConflictoException.class)
                    .hasMessageContaining("No hay stock suficiente")
                    .hasMessageContaining("Disponibles: 5");

            verify(pedidoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rechaza un producto que no existe (404)")
        void rechazaProductoInexistente() {
            when(usuarioRepository.findById(7L)).thenReturn(Optional.of(cliente));
            when(productoRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pedidoService.crearPedido(pedido(7L, 1)))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test
        @DisplayName("Un cliente no puede crear pedidos a nombre de otro (403)")
        void rechazaPedidoDeOtroUsuario() {
            assertThatThrownBy(() -> pedidoService.crearPedidoDeCliente(pedido(99L, 1), 7L))
                    .isInstanceOf(AccessDeniedException.class);

            verify(pedidoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Pagar pedido")
    class PagarPedido {

        private Pedido pendiente;

        @BeforeEach
        void pedidoPendiente() {
            pendiente = new Pedido();
            pendiente.setId(50L);
            pendiente.setUsuario(cliente);
            pendiente.setEstado("pendiente");
            pendiente.setIntentos(0);

            DetallePedido detalle = new DetallePedido();
            detalle.setProductoId(1L);
            detalle.setMarca("Lattafa");
            detalle.setModelo("Eclaire");
            detalle.setCantidad(2);
            pendiente.agregarDetalle(detalle);

            when(pedidoRepository.findById(50L)).thenReturn(Optional.of(pendiente));
        }

        @Test
        @DisplayName("Pago aprobado: queda 'pagado' y descuenta el stock")
        void pagoAprobadoDescuentaStock() {
            when(simuladorPago.aprobar()).thenReturn(true);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(perfume));
            when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

            Pedido resultado = pedidoService.procesarPagoDeCliente(50L, 7L);

            assertThat(resultado.getEstado()).isEqualTo("pagado");
            assertThat(resultado.getIntentos()).isEqualTo(1);
            assertThat(perfume.getStock()).isEqualTo(3); // 5 - 2
        }

        @Test
        @DisplayName("Pago rechazado: queda 'fallido' y el stock no cambia")
        void pagoRechazadoNoTocaStock() {
            when(simuladorPago.aprobar()).thenReturn(false);
            when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

            Pedido resultado = pedidoService.procesarPagoDeCliente(50L, 7L);

            assertThat(resultado.getEstado()).isEqualTo("fallido");
            assertThat(resultado.getIntentos()).isEqualTo(1);
            assertThat(perfume.getStock()).isEqualTo(5);
            verify(productoRepository, never()).save(any());
        }

        @Test
        @DisplayName("No se puede pagar dos veces el mismo pedido (409)")
        void noSePagaDosVeces() {
            pendiente.setEstado("pagado");

            assertThatThrownBy(() -> pedidoService.procesarPagoDeCliente(50L, 7L))
                    .isInstanceOf(ConflictoException.class)
                    .hasMessageContaining("ya fue pagado");
        }

        @Test
        @DisplayName("Un cliente no puede pagar el pedido de otro (403)")
        void noPagaPedidoAjeno() {
            assertThatThrownBy(() -> pedidoService.procesarPagoDeCliente(50L, 99L))
                    .isInstanceOf(AccessDeniedException.class);

            verify(simuladorPago, never()).aprobar();
        }
    }
}
