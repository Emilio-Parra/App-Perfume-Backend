package com.tienda.perfumes.controller;

import com.jayway.jsonpath.JsonPath;
import com.tienda.perfumes.model.Producto;
import com.tienda.perfumes.repository.AdministradorRepository;
import com.tienda.perfumes.repository.PedidoRepository;
import com.tienda.perfumes.repository.ProductoRepository;
import com.tienda.perfumes.repository.UsuarioRepository;
import com.tienda.perfumes.security.JwtService;
import com.tienda.perfumes.service.AdministradorService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración: levantan la aplicación completa (con H2 en memoria)
 * y llaman a la API como lo haría el frontend, con y sin token JWT.
 *
 * Verifican la matriz de permisos de la pauta:
 *   ADMIN    -> acceso total
 *   VENDEDOR -> solo ver productos y órdenes
 *   CLIENTE  -> solo la tienda (comprar)
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API: autenticación JWT y permisos por rol")
class SeguridadApiIntegrationTest {

    private static final String API = "/api/v1";

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwtService;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private AdministradorRepository administradorRepository;
    @Autowired private AdministradorService administradorService;

    private Long productoId;

    @BeforeEach
    void baseDeDatosLimpia() {
        pedidoRepository.deleteAll();
        usuarioRepository.deleteAll();
        productoRepository.deleteAll();
        administradorRepository.deleteAll();

        Producto p = new Producto();
        p.setMarca("Lattafa");
        p.setModelo("Eclaire");
        p.setTipo("arabe");
        p.setGenero("mujer");
        p.setPrecio(30000);
        p.setDescuento(0);
        p.setStock(5);
        p.setVolumen("100 ml");
        p.setImagenes("/img/a.jpg");
        p.setDescripcion("Fragancia gourmand dulce y cremosa.");
        productoId = productoRepository.save(p).getId();
    }

    private String token(String rol) {
        return "Bearer " + jwtService.generarToken("1", rol, Map.of());
    }

    private static final String PRODUCTO_JSON = """
            {"marca":"Rasasi","modelo":"Hawas","tipo":"arabe","genero":"hombre","precio":39990,
             "descuento":0,"stock":8,"volumen":"100 ml","imagenes":"/img/b.jpg",
             "descripcion":"Fragancia acuática y fresca para el día."}
            """;

    @Nested
    @DisplayName("Sin iniciar sesión")
    class Anonimo {

        @Test
        @DisplayName("Puede ver el catálogo (GET público)")
        void veCatalogo() throws Exception {
            mvc.perform(get(API + "/productos")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
            mvc.perform(get(API + "/categorias")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("No puede crear productos ni ver órdenes (401)")
        void noModifica() throws Exception {
            mvc.perform(post(API + "/productos").contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Debes iniciar sesión para acceder a este recurso."));
            mvc.perform(get(API + "/pedidos/admin")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Un token alterado o vencido se rechaza (401)")
        void tokenInvalido() throws Exception {
            mvc.perform(get(API + "/pedidos/admin").header("Authorization", "Bearer no.es.valido"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value(
                            "Tu sesión expiró o el token no es válido. Inicia sesión nuevamente."));
        }
    }

    @Nested
    @DisplayName("Rol ADMIN")
    class Admin {

        @Test
        @DisplayName("Inicia sesión y recibe un token JWT")
        void loginDevuelveToken() throws Exception {
            administradorService.crearAdministradorInicial("admin@duoc.cl", "Root1234");

            String respuesta = mvc.perform(post(API + "/admin/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"correo\":\"admin@duoc.cl\",\"pass\":\"Root1234\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rol").value("admin"))
                    .andReturn().getResponse().getContentAsString();

            String token = JsonPath.read(respuesta, "$.token");
            assertThat(jwtService.validar(token).get(JwtService.CLAIM_ROL)).isEqualTo("ADMIN");

            // Con ese token puede ver su perfil
            mvc.perform(get(API + "/admin/perfil").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.correo").value("admin@duoc.cl"));
        }

        @Test
        @DisplayName("Contraseña incorrecta: 401")
        void loginIncorrecto() throws Exception {
            administradorService.crearAdministradorInicial("admin@duoc.cl", "Root1234");

            mvc.perform(post(API + "/admin/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"correo\":\"admin@duoc.cl\",\"pass\":\"otra\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Puede crear y eliminar productos")
        void gestionaProductos() throws Exception {
            String respuesta = mvc.perform(post(API + "/productos").header("Authorization", token("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.marca").value("Rasasi"))
                    .andReturn().getResponse().getContentAsString();

            Integer id = JsonPath.read(respuesta, "$.id");
            mvc.perform(delete(API + "/productos/" + id).header("Authorization", token("ADMIN")))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Los datos inválidos se rechazan con 400 y el campo con error")
        void validaDatos() throws Exception {
            mvc.perform(post(API + "/productos").header("Authorization", token("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(PRODUCTO_JSON.replace("39990", "-5")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.precio").value("El precio debe ser mayor a 0."));
        }

        @Test
        @DisplayName("Puede ver usuarios y órdenes")
        void veTodo() throws Exception {
            mvc.perform(get(API + "/admin/usuarios").header("Authorization", token("ADMIN"))).andExpect(status().isOk());
            mvc.perform(get(API + "/pedidos/admin").header("Authorization", token("ADMIN"))).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Rol VENDEDOR")
    class Vendedor {

        @Test
        @DisplayName("Puede ver productos (listado y detalle) y órdenes (listado)")
        void veProductosYOrdenes() throws Exception {
            mvc.perform(get(API + "/productos").header("Authorization", token("VENDEDOR"))).andExpect(status().isOk());
            mvc.perform(get(API + "/productos/" + productoId).header("Authorization", token("VENDEDOR")))
                    .andExpect(status().isOk());
            mvc.perform(get(API + "/pedidos/admin").header("Authorization", token("VENDEDOR")))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("No puede crear, borrar ni ver usuarios (403)")
        void noModifica() throws Exception {
            mvc.perform(post(API + "/productos").header("Authorization", token("VENDEDOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_JSON))
                    .andExpect(status().isForbidden());
            mvc.perform(delete(API + "/productos/" + productoId).header("Authorization", token("VENDEDOR")))
                    .andExpect(status().isForbidden());
            mvc.perform(post(API + "/categorias").header("Authorization", token("VENDEDOR"))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"nueva\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(get(API + "/admin/usuarios").header("Authorization", token("VENDEDOR")))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Rol CLIENTE")
    class Cliente {

        @Test
        @DisplayName("No puede ver las órdenes de otros ni el panel (403)")
        void noEntraAlPanel() throws Exception {
            mvc.perform(get(API + "/pedidos/admin").header("Authorization", token("CLIENTE")))
                    .andExpect(status().isForbidden());
            mvc.perform(get(API + "/admin/usuarios").header("Authorization", token("CLIENTE")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Flujo completo: registro → login → compra → pago → stock descontado")
        void flujoDeCompra() throws Exception {
            // 1. Registro
            mvc.perform(post(API + "/usuarios/registro").contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"rut":"123456785","correo":"ana@duoc.cl","nombre":"Ana","apellido":"Paz",
                                     "pass":"abcd","region":"RM","comuna":"Maipú","direccion":"Calle 1"}
                                    """))
                    .andExpect(status().isCreated());

            // 2. Login: devuelve el token
            String login = mvc.perform(post(API + "/usuarios/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"correo\":\"ana@duoc.cl\",\"pass\":\"abcd\"}"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String token = "Bearer " + JsonPath.read(login, "$.token");
            Integer usuarioId = JsonPath.read(login, "$.id");

            // 3. Compra de 2 unidades
            String pedido = mvc.perform(post(API + "/pedidos").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"usuarioId":%d,"items":[{"id":%d,"cantidad":2}],
                                     "cliente":{"nombre":"Ana","apellido":"Paz","correo":"ana@duoc.cl"},
                                     "envio":{"calle":"Calle 1","region":"RM","comuna":"Maipú"}}
                                    """.formatted(usuarioId, productoId)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.total").value(60000))
                    .andExpect(jsonPath("$.estado").value("pendiente"))
                    .andReturn().getResponse().getContentAsString();
            Integer pedidoId = JsonPath.read(pedido, "$.id");

            // 4. Pago (en los tests siempre se aprueba)
            mvc.perform(post(API + "/pedidos/" + pedidoId + "/pago").header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("pagado"));

            // 5. El stock bajó de 5 a 3
            assertThat(productoRepository.findById(productoId).orElseThrow().getStock()).isEqualTo(3);
        }

        @Test
        @DisplayName("El registro con un RUN inválido se rechaza (400)")
        void registroRutInvalido() throws Exception {
            mvc.perform(post(API + "/usuarios/registro").contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"rut":"123456789","correo":"ana@duoc.cl","nombre":"Ana","apellido":"Paz",
                                     "pass":"abcd","direccion":"Calle 1"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.rut").exists());
        }
    }
}
