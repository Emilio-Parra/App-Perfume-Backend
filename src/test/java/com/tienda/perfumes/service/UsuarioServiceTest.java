package com.tienda.perfumes.service;

import com.tienda.perfumes.dto.RegistroRequest;
import com.tienda.perfumes.dto.UsuarioAdminRequest;
import com.tienda.perfumes.exception.ConflictoException;
import com.tienda.perfumes.model.Pedido;
import com.tienda.perfumes.model.Usuario;
import com.tienda.perfumes.repository.PedidoRepository;
import com.tienda.perfumes.repository.UsuarioRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService: registro, login y gestión de roles")
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PedidoRepository pedidoRepository;

    @InjectMocks private UsuarioService usuarioService;

    private RegistroRequest registro(String correo, String rut) {
        return new RegistroRequest(rut, correo, "Ana", "Paz", "abcd",
                "2000-01-01", "RM", "Maipú", "Calle 1");
    }

    private UsuarioAdminRequest datosAdmin(String rol, String pass) {
        return new UsuarioAdminRequest("111111111", "vale@duoc.cl", "Vale", "Rojas",
                pass, rol, null, null, null, "Local 1");
    }

    @Test
    @DisplayName("Registro: guarda la contraseña cifrada con BCrypt, nunca en texto plano")
    void registroCifraContrasena() {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario nuevo = usuarioService.registrar(registro("  ANA@duoc.cl ", "123456785"));

        assertThat(nuevo.getPasswordHash()).isNotEqualTo("abcd").startsWith("$2");
        assertThat(new BCryptPasswordEncoder().matches("abcd", nuevo.getPasswordHash())).isTrue();
        assertThat(nuevo.getCorreo()).isEqualTo("ana@duoc.cl"); // normalizado
        assertThat(nuevo.getRol()).isEqualTo("cliente");        // registro público = cliente
    }

    @Test
    @DisplayName("Registro: rechaza un correo ya registrado (409)")
    void rechazaCorreoDuplicado() {
        when(usuarioRepository.existsByCorreo("ana@duoc.cl")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.registrar(registro("ana@duoc.cl", "123456785")))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("correo");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registro: rechaza un RUT ya registrado (409)")
    void rechazaRutDuplicado() {
        when(usuarioRepository.existsByRut("123456785")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.registrar(registro("ana@duoc.cl", "123456785")))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("RUT");
    }

    @Test
    @DisplayName("Login: acepta la contraseña correcta y rechaza una incorrecta")
    void autenticar() {
        Usuario guardado = new Usuario();
        guardado.setCorreo("ana@duoc.cl");
        guardado.setPasswordHash(new BCryptPasswordEncoder().encode("abcd"));
        when(usuarioRepository.findByCorreo("ana@duoc.cl")).thenReturn(Optional.of(guardado));

        assertThat(usuarioService.autenticar("ANA@duoc.cl", "abcd")).isPresent();
        assertThat(usuarioService.autenticar("ana@duoc.cl", "otra")).isEmpty();
    }

    @Test
    @DisplayName("Panel: el admin puede crear un VENDEDOR")
    void creaVendedor() {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario vendedor = usuarioService.crearUsuario(datosAdmin("Vendedor", "vend123"));

        assertThat(vendedor.getRol()).isEqualTo("vendedor");
    }

    @Test
    @DisplayName("Panel: sin rol, el usuario se crea como cliente")
    void rolPorDefectoCliente() {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(usuarioService.crearUsuario(datosAdmin(null, "abcd")).getRol()).isEqualTo("cliente");
    }

    @Test
    @DisplayName("Panel: NO se puede crear un administrador desde el formulario")
    void noCreaAdministradores() {
        assertThatThrownBy(() -> usuarioService.crearUsuario(datosAdmin("admin", "abcd")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rol no permitido");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Panel: la contraseña es obligatoria al crear")
    void contrasenaObligatoriaAlCrear() {
        assertThatThrownBy(() -> usuarioService.crearUsuario(datosAdmin("cliente", "")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Panel: no se elimina un usuario que tiene pedidos (409)")
    void noEliminaUsuarioConPedidos() {
        Usuario usuario = new Usuario();
        usuario.setId(3L);
        usuario.setRol("cliente");
        when(usuarioRepository.findByCorreo("ana@duoc.cl")).thenReturn(Optional.of(usuario));
        when(pedidoRepository.findByUsuarioId(3L)).thenReturn(List.of(new Pedido()));

        assertThatThrownBy(() -> usuarioService.eliminarUsuario("ana@duoc.cl"))
                .isInstanceOf(ConflictoException.class);

        verify(usuarioRepository, never()).delete(any());
    }
}
