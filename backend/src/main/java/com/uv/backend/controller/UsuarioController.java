package com.uv.backend.controller;

import com.uv.backend.entity.Usuario;
import com.uv.backend.repository.IUsuario;
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

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private static final Set<String> ROLES = Set.of("MAESTRO", "ALUMNO", "ADMIN");

    private final IUsuario iUsuario;

    public UsuarioController(IUsuario iUsuario) {
        this.iUsuario = iUsuario;
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

    // Listar usuarios. Filtros: ?rol= (MAESTRO/ALUMNO/ADMIN), ?q= (nombre o email)
    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) String rol,
            @RequestParam(required = false) String q) {
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

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody UsuarioRequest req) {
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

    // Editar usuario (PUT /usuarios/{id})
    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Integer id, @RequestBody UsuarioEditRequest req) {
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
            u.setRol(rol);
        }
        return ResponseEntity.ok(UsuarioResponse.de(iUsuario.save(u)));
    }

    // Cambiar contraseña (PATCH /usuarios/{id}/password)
    @PatchMapping("/{id}/password")
    public ResponseEntity<?> cambiarPassword(@PathVariable Integer id, @RequestBody PasswordRequest req) {
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

    // Cambiar rol (PATCH /usuarios/{id}/rol)
    @PatchMapping("/{id}/rol")
    public ResponseEntity<?> cambiarRol(@PathVariable Integer id, @RequestBody RolRequest req) {
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
        u.setRol(rol);
        return ResponseEntity.ok(UsuarioResponse.de(iUsuario.save(u)));
    }

    // Eliminar usuario (DELETE /usuarios/{id})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
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
