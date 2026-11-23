package com.uv.backend.controller;

import com.uv.backend.entity.Usuario;
import com.uv.backend.repository.IUsuario;
import com.uv.backend.security.AuthInterceptor;
import com.uv.backend.security.Permisos;
import com.uv.backend.security.TokenService.DatosToken;
import com.uv.backend.service.Filtros;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/**
 * Usuarios. Permisos (segun el usuario del token):
 * - Listar, crear, cambiar rol y eliminar: solo ADMIN.
 * - Editar datos y cambiar contrasena: ADMIN o el propio usuario.
 * - Nadie puede cambiar su propio rol ni eliminar su propia cuenta.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private static final Set<String> ROLES = Set.of("MAESTRO", "ALUMNO", "ADMIN");
    private static final String SESION = AuthInterceptor.ATRIBUTO;

    private final IUsuario iUsuario;
    private final Permisos permisos;

    public UsuarioController(IUsuario iUsuario, Permisos permisos) {
        this.iUsuario = iUsuario;
        this.permisos = permisos;
    }

    // Datos que llegan en el POST
    public record UsuarioRequest(String nombre, String email, String password, String rol) {}

    // Datos para editar (PUT /usuarios/{id}): solo los campos no null se actualizan
    public record UsuarioEditRequest(String nombre, String email, String rol) {}

    // Datos para cambiar contraseña (PATCH /usuarios/{id}/password)
    public record PasswordRequest(String password) {}

    // Datos para cambiar rol (PATCH /usuarios/{id}/rol)
    public record RolRequest(String rol) {}

    // Datos que se devuelven (sin la contrasena)
    public record UsuarioResponse(Integer idUsuario, String nombre, String email, String rol, LocalDateTime fechaRegistro) {
        static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.getIdUsuario(), u.getNombre(), u.getEmail(), u.getRol(), u.getFechaRegistro());
        }
    }

    private static ResponseEntity<String> prohibido(String mensaje) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mensaje);
    }

    // Listar usuarios (solo ADMIN). Filtros: ?rol= (MAESTRO/ALUMNO/ADMIN), ?q= (nombre o email)
    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) String rol,
            @RequestParam(required = false) String q,
            @RequestAttribute(name = SESION, required = false) DatosToken sesion) {
        if (!permisos.esAdmin(sesion)) {
            return prohibido("Solo el administrador puede ver la lista de usuarios");
        }
        if (rol != null && !ROLES.contains(rol.toUpperCase())) {
            return ResponseEntity.badRequest().body("rol debe ser MAESTRO, ALUMNO o ADMIN");
        }
        List<UsuarioResponse> usuarios = iUsuario.findAll().stream()
                .filter(u -> rol == null || u.getRol().equalsIgnoreCase(rol))
                .filter(u -> Filtros.contiene(q, u.getNombre(), u.getEmail()))
                .map(UsuarioResponse::de)
                .toList();
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtener(@PathVariable Integer id) {
        return iUsuario.findById(id)
                .map(u -> ResponseEntity.ok(UsuarioResponse.de(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    // Crear usuario (solo ADMIN; el registro publico es POST /auth/registro)
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody UsuarioRequest req,
            @RequestAttribute(name = SESION, required = false) DatosToken sesion) {
        if (!permisos.esAdmin(sesion)) {
            return prohibido("Solo el administrador puede crear usuarios");
        }
        if (vacio(req.nombre()) || vacio(req.email()) || vacio(req.password()) || vacio(req.rol())) {
            return ResponseEntity.badRequest().body("nombre, email, password y rol son obligatorios");
        }
        String rol = req.rol().toUpperCase();
        if (!ROLES.contains(rol)) {
            return ResponseEntity.badRequest().body("rol debe ser MAESTRO, ALUMNO o ADMIN");
        }
        if (iUsuario.existsByEmail(req.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("El email ya esta registrado");
        }

        Usuario u = new Usuario();
        u.setNombre(req.nombre());
        u.setEmail(req.email());
        u.setPasswordHash(sha256(req.password()));
        u.setRol(rol);
        Usuario guardado = iUsuario.save(u);

        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.de(guardado));
    }

    // Editar usuario (PUT /usuarios/{id}): ADMIN o el propio usuario; el rol solo lo cambia un ADMIN
    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Integer id, @RequestBody UsuarioEditRequest req,
            @RequestAttribute(name = SESION, required = false) DatosToken sesion) {
        boolean esAdmin = permisos.esAdmin(sesion);
        if (!esAdmin && !permisos.esElMismo(sesion, id)) {
            return prohibido("Solo puedes editar tu propia cuenta");
        }
        var opt = iUsuario.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Usuario u = opt.get();
        if (req.nombre() != null && !req.nombre().isBlank()) {
            u.setNombre(req.nombre());
        }
        if (req.email() != null && !req.email().isBlank() && !req.email().equals(u.getEmail())) {
            if (iUsuario.existsByEmail(req.email())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("El email ya esta registrado");
            }
            u.setEmail(req.email());
        }
        if (req.rol() != null) {
            String rol = req.rol().toUpperCase();
            if (!ROLES.contains(rol)) {
                return ResponseEntity.badRequest().body("rol debe ser MAESTRO, ALUMNO o ADMIN");
            }
            if (!rol.equals(u.getRol())) {
                if (!esAdmin) {
                    return prohibido("Solo el administrador puede cambiar roles");
                }
                if (sesion != null && sesion.idUsuario().equals(id)) {
                    return prohibido("No puedes cambiar tu propio rol");
                }
                u.setRol(rol);
            }
        }
        return ResponseEntity.ok(UsuarioResponse.de(iUsuario.save(u)));
    }

    // Cambiar contraseña (PATCH /usuarios/{id}/password): ADMIN o el propio usuario
    @PatchMapping("/{id}/password")
    public ResponseEntity<?> cambiarPassword(@PathVariable Integer id, @RequestBody PasswordRequest req,
            @RequestAttribute(name = SESION, required = false) DatosToken sesion) {
        if (!permisos.esAdmin(sesion) && !permisos.esElMismo(sesion, id)) {
            return prohibido("Solo puedes cambiar tu propia contraseña");
        }
        if (vacio(req.password())) {
            return ResponseEntity.badRequest().body("password es obligatorio");
        }
        var opt = iUsuario.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Usuario u = opt.get();
        u.setPasswordHash(sha256(req.password()));
        return ResponseEntity.ok(UsuarioResponse.de(iUsuario.save(u)));
    }

    // Cambiar rol (PATCH /usuarios/{id}/rol): solo ADMIN y no sobre si mismo
    @PatchMapping("/{id}/rol")
    public ResponseEntity<?> cambiarRol(@PathVariable Integer id, @RequestBody RolRequest req,
            @RequestAttribute(name = SESION, required = false) DatosToken sesion) {
        if (!permisos.esAdmin(sesion)) {
            return prohibido("Solo el administrador puede cambiar roles");
        }
        if (vacio(req.rol())) {
            return ResponseEntity.badRequest().body("rol es obligatorio");
        }
        String rol = req.rol().toUpperCase();
        if (!ROLES.contains(rol)) {
            return ResponseEntity.badRequest().body("rol debe ser MAESTRO, ALUMNO o ADMIN");
        }
        var opt = iUsuario.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Usuario u = opt.get();
        if (sesion != null && sesion.idUsuario().equals(id) && !rol.equals(u.getRol())) {
            return prohibido("No puedes cambiar tu propio rol");
        }
        u.setRol(rol);
        return ResponseEntity.ok(UsuarioResponse.de(iUsuario.save(u)));
    }

    // Eliminar usuario (DELETE /usuarios/{id}): solo ADMIN y no su propia cuenta
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id,
            @RequestAttribute(name = SESION, required = false) DatosToken sesion) {
        if (!permisos.esAdmin(sesion)) {
            return prohibido("Solo el administrador puede eliminar usuarios");
        }
        if (sesion != null && sesion.idUsuario().equals(id)) {
            return prohibido("No puedes eliminar tu propia cuenta");
        }
        if (!iUsuario.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        iUsuario.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    static String sha256(String texto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
