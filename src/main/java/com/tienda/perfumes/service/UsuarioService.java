
package com.tienda.perfumes.service;

import com.tienda.perfumes.dto.RegistroRequest;
import com.tienda.perfumes.dto.UsuarioAdminRequest;
import com.tienda.perfumes.exception.ConflictoException;
import com.tienda.perfumes.exception.RecursoNoEncontradoException;
import com.tienda.perfumes.model.Usuario;
import com.tienda.perfumes.repository.PedidoRepository;
import com.tienda.perfumes.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class UsuarioService {

    public static final String ROL_CLIENTE = "cliente";

    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PedidoRepository pedidoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
    }

    // ---------- Registro y login ----------

    // Registro público desde la tienda
    @Transactional
    public Usuario registrar(RegistroRequest datos) {
        Usuario nuevo = new Usuario();
        nuevo.setRut(datos.rut());
        nuevo.setCorreo(datos.correo());
        nuevo.setNombre(datos.nombre());
        nuevo.setApellido(datos.apellido());
        nuevo.setFechaNac(datos.fechaNac());
        nuevo.setRegion(datos.region());
        nuevo.setComuna(datos.comuna());
        nuevo.setDireccion(datos.direccion());
        return registrarCliente(nuevo, datos.pass());
    }

    // Registrar un nuevo cliente (todas las cuentas públicas son clientes)
    @Transactional
    public Usuario registrarCliente(Usuario datos, String contrasena) {

        if (datos == null ||
                datos.getCorreo() == null ||
                datos.getCorreo().isBlank() ||
                datos.getRut() == null ||
                datos.getRut().isBlank() ||
                contrasena == null ||
                contrasena.isBlank()) {
            throw new IllegalArgumentException(
                    "Correo, RUT y contraseña son obligatorios."
            );
        }

        validarLargoContrasena(contrasena);

        String correo = normalizarCorreo(datos.getCorreo());
        String rut = normalizarRut(datos.getRut());

        // Validar correo duplicado
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ConflictoException("El correo ya está registrado.");
        }

        // Validar RUT duplicado
        if (usuarioRepository.existsByRut(rut)) {
            throw new ConflictoException("El RUT ya está registrado.");
        }

        Usuario nuevo = new Usuario();

        nuevo.setRut(rut);
        nuevo.setCorreo(correo);
        nuevo.setNombre(recortar(datos.getNombre()));
        nuevo.setApellido(recortar(datos.getApellido()));
        nuevo.setFechaNac(datos.getFechaNac());
        nuevo.setRegion(datos.getRegion());
        nuevo.setComuna(datos.getComuna());
        nuevo.setDireccion(recortar(datos.getDireccion()));

        // Todos los registros públicos serán clientes
        nuevo.setRol(ROL_CLIENTE);

        // Guardar la contraseña protegida, nunca en texto plano
        nuevo.setPasswordHash(passwordEncoder.encode(contrasena));

        return usuarioRepository.save(nuevo);
    }

    // Validar las credenciales para iniciar sesión
    @Transactional(readOnly = true)
    public Optional<Usuario> autenticar(String correo, String contrasena) {

        if (correo == null || contrasena == null) {
            return Optional.empty();
        }

        return usuarioRepository
                .findByCorreo(normalizarCorreo(correo))
                .filter(usuario ->
                        passwordEncoder.matches(contrasena, usuario.getPasswordHash())
                );
    }

    // Buscar un usuario por correo
    @Transactional(readOnly = true)
    public Optional<Usuario> obtenerPorCorreo(String correo) {
        if (correo == null) {
            return Optional.empty();
        }
        return usuarioRepository.findByCorreo(normalizarCorreo(correo));
    }

    // Contar los clientes registrados en MySQL
    @Transactional(readOnly = true)
    public long contarClientes() {
        return usuarioRepository.countByRol(ROL_CLIENTE);
    }

    // ---------- Gestión de clientes desde el panel admin ----------

    @Transactional(readOnly = true)
    public List<Usuario> listarClientes() {
        return usuarioRepository.findAll().stream()
                .filter(u -> ROL_CLIENTE.equalsIgnoreCase(u.getRol()))
                .toList();
    }

    @Transactional
    public Usuario crearCliente(UsuarioAdminRequest datos) {
        validarRolCliente(datos.rol(), "Solo se pueden crear clientes desde esta sección.");

        if (datos.pass() == null || datos.pass().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria para un cliente nuevo.");
        }

        Usuario nuevo = new Usuario();
        nuevo.setRut(datos.rut());
        nuevo.setCorreo(datos.correo());
        nuevo.setNombre(datos.nombre());
        nuevo.setApellido(datos.apellido());
        nuevo.setFechaNac(datos.fechaNac());
        nuevo.setRegion(datos.region());
        nuevo.setComuna(datos.comuna());
        nuevo.setDireccion(datos.direccion());

        return registrarCliente(nuevo, datos.pass());
    }

    @Transactional
    public Usuario actualizarCliente(String correoOriginal, UsuarioAdminRequest datos) {
        validarRolCliente(datos.rol(), "No se puede asignar el rol de administrador desde aquí.");

        Usuario usuario = buscarCliente(correoOriginal);

        String nuevoCorreo = normalizarCorreo(datos.correo());
        String nuevoRut = normalizarRut(datos.rut());

        if (!nuevoCorreo.equals(usuario.getCorreo()) && usuarioRepository.existsByCorreo(nuevoCorreo)) {
            throw new ConflictoException("Ese correo ya está en uso.");
        }
        if (!nuevoRut.equals(usuario.getRut()) && usuarioRepository.existsByRut(nuevoRut)) {
            throw new ConflictoException("Ese RUN ya está en uso.");
        }

        usuario.setCorreo(nuevoCorreo);
        usuario.setRut(nuevoRut);
        usuario.setNombre(recortar(datos.nombre()));
        usuario.setApellido(recortar(datos.apellido()));
        usuario.setFechaNac(datos.fechaNac());
        usuario.setRegion(datos.region());
        usuario.setComuna(datos.comuna());
        usuario.setDireccion(recortar(datos.direccion()));

        // Contraseña vacía = se conserva la actual
        if (datos.pass() != null && !datos.pass().isBlank()) {
            validarLargoContrasena(datos.pass());
            usuario.setPasswordHash(passwordEncoder.encode(datos.pass()));
        }

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void eliminarCliente(String correo) {
        Usuario usuario = buscarCliente(correo);

        // No borrar clientes con pedidos: sus boletas deben conservarse
        if (!pedidoRepository.findByUsuarioId(usuario.getId()).isEmpty()) {
            throw new ConflictoException("Este cliente tiene pedidos. No se puede eliminar.");
        }

        usuarioRepository.delete(usuario);
    }

    // ---------- Reglas internas ----------

    private Usuario buscarCliente(String correo) {
        return usuarioRepository.findByCorreo(normalizarCorreo(correo))
                .filter(u -> ROL_CLIENTE.equalsIgnoreCase(u.getRol()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado."));
    }

    private void validarRolCliente(String rol, String mensaje) {
        if (rol != null && !rol.isBlank() && !ROL_CLIENTE.equalsIgnoreCase(rol.trim())) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    private void validarLargoContrasena(String contrasena) {
        if (contrasena.length() < 4 || contrasena.length() > 10) {
            throw new IllegalArgumentException("La contraseña debe tener entre 4 y 10 caracteres.");
        }
    }

    private String normalizarCorreo(String correo) {
        return correo == null ? null : correo.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizarRut(String rut) {
        // Se guarda tal como lo escribió el usuario para no romper RUN ya registrados
        return rut == null ? null : rut.trim();
    }

    private String recortar(String texto) {
        return texto == null ? null : texto.trim();
    }
}
