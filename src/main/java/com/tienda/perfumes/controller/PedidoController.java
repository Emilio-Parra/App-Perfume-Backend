
package com.tienda.perfumes.controller;

import com.tienda.perfumes.config.ApiRutas;
import com.tienda.perfumes.dto.CrearPedidoRequest;
import com.tienda.perfumes.model.Pedido;
import com.tienda.perfumes.service.PedidoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Pedidos (Boletas)", description = "Creación, pago y consulta de pedidos")
@RequestMapping(ApiRutas.V1 + "/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // Obtener todos los pedidos para el administrador
    @Operation(summary = "Listar todos los pedidos con su detalle (panel administrador)")
    @GetMapping("/admin")
    public List<Map<String, Object>> obtenerPedidosAdmin() {
        return pedidoService.obtenerPedidosAdmin();
    }

    // Crear un pedido para el cliente autenticado
    @Operation(summary = "Crear un pedido (boleta) para el cliente autenticado")
    @PostMapping
    public ResponseEntity<Map<String, Object>> crearPedido(
            @Valid @RequestBody CrearPedidoRequest datos,
            Authentication authentication) {

        // El ID del cliente sale de la sesión, no del navegador
        Pedido pedido = pedidoService.crearPedidoDeCliente(
                datos, usuarioAutenticado(authentication));

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "id", pedido.getId(),
                        "codigo", pedido.getCodigo(),
                        "estado", pedido.getEstado(),
                        "fecha", pedido.getFecha().toString(),
                        "total", pedido.getTotal(),
                        "cantidadProductos", pedido.getDetalles().size()
                ));
    }

    // Procesar el pago de un pedido propio
    @Operation(summary = "Procesar (simular) el pago de un pedido propio")
    @PostMapping("/{id}/pago")
    public Map<String, Object> procesarPago(
            @PathVariable Long id,
            Authentication authentication) {

        Pedido pedido = pedidoService.procesarPagoDeCliente(
                id, usuarioAutenticado(authentication));

        return Map.of(
                "id", pedido.getId(),
                "codigo", pedido.getCodigo(),
                "estado", pedido.getEstado(),
                "intentos", pedido.getIntentos(),
                "total", pedido.getTotal()
        );
    }

    // El cliente se identifica en Spring Security con su ID de MySQL
    private Long usuarioAutenticado(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
