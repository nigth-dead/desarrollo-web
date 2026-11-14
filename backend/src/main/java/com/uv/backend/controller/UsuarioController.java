package com.uv.backend.controller;

import com.uv.backend.model.Usuario;
import com.uv.backend.repository.UsuarioRepository;
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

    private final UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // Datos que llegan en el POST
    public record UsuarioRequest(String nombre, String email, String password, String rol) {}

    // Datos que se devuelven (sin la contrasena)
    public record UsuarioResponse(Integer idUsuario, String nombre, String email, String rol, LocalDateTime fechaRegistro) {
        static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.getIdUsuario(), u.getNombre(), u.getEmail(), u.getRol(), u.getFechaRegistro());
        }
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(UsuarioResponse::de).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtener(@PathVariable Integer id) {
        return usuarioRepository.findById(id)
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
        if (usuarioRepository.existsByEmail(req.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("El email ya esta registrado");
        }

        Usuario u = new Usuario();
        u.setNombre(req.nombre());
        u.setEmail(req.email());
        u.setPasswordHash(sha256(req.password()));
        u.setRol(rol);
        Usuario guardado = usuarioRepository.save(u);

        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.de(guardado));
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    private static String sha256(String texto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
