
package com.tienda.perfumes.service;

import com.tienda.perfumes.dto.CrearPedidoRequest;
import com.tienda.perfumes.exception.ConflictoException;
import com.tienda.perfumes.exception.RecursoNoEncontradoException;
import com.tienda.perfumes.model.DetallePedido;
import com.tienda.perfumes.model.Pedido;
import com.tienda.perfumes.model.Producto;
import com.tienda.perfumes.model.Usuario;

import com.tienda.perfumes.repository.PedidoRepository;
import com.tienda.perfumes.repository.ProductoRepository;
import com.tienda.perfumes.repository.UsuarioRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import java.util.Map;
import java.util.LinkedHashMap;


@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimuladorPago simuladorPago;

    public PedidoService(
            PedidoRepository pedidoRepository,
            ProductoRepository productoRepository,
            UsuarioRepository usuarioRepository,
            SimuladorPago simuladorPago) {

        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.simuladorPago = simuladorPago;
    }

    // Obtener todos los pedidos
    public List<Pedido> obtenerTodos() {
        return pedidoRepository.findAll();
    }

    // Buscar un pedido por ID
    public Optional<Pedido> obtenerPorId(Long id) {
        return pedidoRepository.findById(id);
    }

    // Obtener pedidos de un cliente
    public List<Pedido> obtenerPorUsuario(Long usuarioId) {
        return pedidoRepository.findByUsuarioId(usuarioId);
    }

    // Buscar pedidos según su estado
    public List<Pedido> obtenerPorEstado(String estado) {
        return pedidoRepository.findByEstado(estado);
    }

    // Crear un pedido comprobando que sea del cliente que inició sesión
    @Transactional
    public Pedido crearPedidoDeCliente(CrearPedidoRequest datos, Long usuarioAutenticadoId) {
        if (datos.usuarioId() == null || !datos.usuarioId().equals(usuarioAutenticadoId)) {
            throw new AccessDeniedException("No puedes crear pedidos para otro usuario.");
        }
        return crearPedido(datos);
    }

    // Pagar un pedido comprobando que sea del cliente que inició sesión
    @Transactional
    public Pedido procesarPagoDeCliente(Long pedidoId, Long usuarioAutenticadoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El pedido no existe."));

        if (pedido.getUsuario() == null
                || !pedido.getUsuario().getId().equals(usuarioAutenticadoId)) {
            throw new AccessDeniedException("No puedes pagar un pedido de otro usuario.");
        }
        return procesarPago(pedidoId);
    }

    // Crear un pedido con sus productos
    @Transactional
    public Pedido crearPedido(CrearPedidoRequest datos) {

        if (datos == null || datos.usuarioId() == null ||
                datos.items() == null || datos.items().isEmpty()) {
            throw new IllegalArgumentException(
                    "El pedido debe tener un cliente y productos."
            );
        }

        // Buscar al cliente en MySQL
        Usuario usuario = usuarioRepository
                .findById(datos.usuarioId())
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "El usuario no existe."
                        )
                );

        Pedido pedido = new Pedido();

        pedido.setUsuario(usuario);

        // Guardar con el pedido la dirección informada en el checkout.
        if (datos.cliente() == null || datos.envio() == null) {
            throw new IllegalArgumentException(
                    "Debes completar los datos del cliente y de entrega."
            );
        }

        pedido.setClienteNombre(datos.cliente().nombre());
        pedido.setClienteApellido(datos.cliente().apellido());
        pedido.setClienteCorreo(datos.cliente().correo());

        pedido.setEnvioCalle(datos.envio().calle());
        pedido.setEnvioDepartamento(datos.envio().departamento());
        pedido.setEnvioRegion(datos.envio().region());
        pedido.setEnvioComuna(datos.envio().comuna());
        pedido.setEnvioIndicaciones(datos.envio().indicaciones());

        pedido.setCodigo("ORDER" +
                UUID.randomUUID().toString()
                        .replace("-", "")
                        .substring(0, 10)
                        .toUpperCase());

        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado("pendiente");
        pedido.setIntentos(0);

        int total = 0;

        // Recorrer los productos solicitados
        for (CrearPedidoRequest.ItemPedidoRequest item : datos.items()) {

            if (item == null || item.id() == null ||
                    item.cantidad() == null || item.cantidad() <= 0) {
                throw new IllegalArgumentException(
                        "Producto o cantidad no válida."
                );
            }

            // Buscar el precio real del producto
            Producto producto = productoRepository
                    .findById(item.id())
                    .orElseThrow(() ->
                            new RecursoNoEncontradoException(
                                    "El producto con ID " + item.id() + " ya no existe."
                            )
                    );

            if (producto.getPrecio() == null ||
                    producto.getPrecio() < 0) {
                throw new IllegalArgumentException(
                        "El producto no tiene un precio válido."
                );
            }

            if (producto.getStock() == null ||
                    producto.getStock() < item.cantidad()) {
                throw new ConflictoException(
                        "No hay stock suficiente de " + producto.getMarca() + " "
                                + producto.getModelo() + ". Disponibles: "
                                + (producto.getStock() == null ? 0 : producto.getStock()) + "."
                );
            }

            int descuento = producto.getDescuento() == null
                    ? 0 : producto.getDescuento();

            if (descuento < 0 || descuento > 100) {
                throw new IllegalArgumentException(
                        "El descuento del producto no es válido."
                );
            }

            int precioFinal = (int) Math.round(
                    producto.getPrecio() * (100 - descuento) / 100.0
            );

            // Guardar una copia de los datos del perfume
            DetallePedido detalle = new DetallePedido();

            detalle.setProductoId(producto.getId());
            detalle.setMarca(producto.getMarca());
            detalle.setModelo(producto.getModelo());
            detalle.setTipo(producto.getTipo());
            detalle.setImagenes(producto.getImagenes());
            detalle.setPrecio(precioFinal);
            detalle.setCantidad(item.cantidad());

            pedido.agregarDetalle(detalle);

            total = Math.addExact(
                    total,
                    Math.multiplyExact(precioFinal, item.cantidad())
            );
        }

        pedido.setTotal(total);

        // Guardar pedido y detalles en MySQL
        return pedidoRepository.save(pedido);
    }

    
    // Simular el pago de un pedido
    @Transactional
    public Pedido procesarPago(Long pedidoId) {

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "El pedido no existe."
                        )
                );

        // Evitar descontar el stock dos veces
        if ("pagado".equals(pedido.getEstado())) {
            throw new ConflictoException(
                    "Este pedido ya fue pagado."
            );
        }

        // Simular el resultado del pago (por defecto 70% de éxito)
        boolean pagoExitoso = simuladorPago.aprobar();

        if (pagoExitoso) {

            // Descontar los productos comprados
            for (DetallePedido detalle : pedido.getDetalles()) {

                Producto producto = productoRepository
                        .findById(detalle.getProductoId())
                        .orElseThrow(() ->
                                new RecursoNoEncontradoException(
                                        "El producto " + detalle.getMarca() + " "
                                                + detalle.getModelo() + " ya no existe."
                                )
                        );

                if (producto.getStock() == null ||
                        producto.getStock() < detalle.getCantidad()) {
                    throw new ConflictoException(
                            "Stock insuficiente de " + producto.getMarca() + " "
                                    + producto.getModelo() + " para completar el pago."
                    );
                }

                producto.setStock(
                        producto.getStock() - detalle.getCantidad()
                );

                productoRepository.save(producto);
            }

            pedido.setEstado("pagado");

        } else {
            pedido.setEstado("fallido");
        }

        pedido.setIntentos(pedido.getIntentos() + 1);

        return pedidoRepository.save(pedido);
    }


    // Preparar los pedidos para mostrarlos en el panel (admin y vendedor)
    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerPedidosAdmin() {
        return pedidoRepository.findAll().stream()
                .map(this::convertirParaPanel)
                .toList();
    }

    // Detalle de un pedido para el panel (admin y vendedor)
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDetalleAdmin(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el pedido con ID " + id + "."));
        return convertirParaPanel(pedido);
    }

    // Datos del pedido, del cliente, del envío y de cada producto comprado
    private Map<String, Object> convertirParaPanel(Pedido pedido) {
        Map<String, Object> datos = new LinkedHashMap<>();

        datos.put("id", pedido.getId());
        datos.put("codigo", pedido.getCodigo());
        datos.put("fecha", pedido.getFecha().toString());
        datos.put("estado", pedido.getEstado());
        datos.put("intentos", pedido.getIntentos());
        datos.put("total", pedido.getTotal());

        // Incluir la información de entrega también en las consultas admin.
        Map<String, Object> cliente = new LinkedHashMap<>();
        cliente.put("nombre", pedido.getClienteNombre());
        cliente.put("apellido", pedido.getClienteApellido());
        cliente.put("correo", pedido.getClienteCorreo());
        datos.put("cliente", cliente);

        Map<String, Object> envio = new LinkedHashMap<>();
        envio.put("calle", pedido.getEnvioCalle());
        envio.put("departamento", pedido.getEnvioDepartamento());
        envio.put("region", pedido.getEnvioRegion());
        envio.put("comuna", pedido.getEnvioComuna());
        envio.put("indicaciones", pedido.getEnvioIndicaciones());
        datos.put("envio", envio);

        datos.put(
            "usuarioId",
            pedido.getUsuario().getId()
        );

        // Productos guardados en cada pedido
        datos.put(
            "items",
            pedido.getDetalles().stream().map(detalle -> {

                Map<String, Object> item = new LinkedHashMap<>();

                item.put("id", detalle.getProductoId());
                item.put("marca", detalle.getMarca());
                item.put("modelo", detalle.getModelo());
                item.put("tipo", detalle.getTipo());
                item.put("imagenes", detalle.getImagenes());
                item.put("precio", detalle.getPrecio());
                item.put("cantidad", detalle.getCantidad());

                return item;

            }).toList()
        );

        return datos;
    }

}
