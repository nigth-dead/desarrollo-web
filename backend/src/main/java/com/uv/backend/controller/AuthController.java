package com.uv.backend.controller;

import com.uv.backend.controller.UsuarioController.UsuarioResponse;
import com.uv.backend.entity.Usuario;
import com.uv.backend.repository.IUsuario;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // ADMIN no se puede elegir en el registro publico
    private static final Set<String> ROLES_REGISTRO = Set.of("MAESTRO", "ALUMNO");

    private final IUsuario iUsuario;

    public AuthController(IUsuario iUsuario) {
        this.iUsuario = iUsuario;
    }

    public record RegistroRequest(String nombre, String email, String password, String rol) {}

    public record LoginRequest(String email, String password) {}

    // Registrarse (POST /auth/registro)
    @PostMapping("/registro")
    public ResponseEntity<?> registro(@RequestBody RegistroRequest req) {
        if (vacio(req.nombre()) || vacio(req.email()) || vacio(req.password()) || vacio(req.rol())) {
            return ResponseEntity.badRequest().body("nombre, email, password y rol son obligatorios");
        }
        String rol = req.rol().toUpperCase();
        if (!ROLES_REGISTRO.contains(rol)) {
            return ResponseEntity.badRequest().body("rol debe ser MAESTRO o ALUMNO");
        }
        if (iUsuario.existsByEmail(req.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("El email ya esta registrado");
        }
        Usuario u = new Usuario(req.nombre(), req.email(), UsuarioController.sha256(req.password()), rol);
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.de(iUsuario.save(u)));
    }

    // Iniciar sesion (POST /auth/login)
    // Por ahora devuelve los datos del usuario; el token JWT queda pendiente
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (vacio(req.email()) || vacio(req.password())) {
            return ResponseEntity.badRequest().body("email y password son obligatorios");
        }
        Optional<Usuario> opt = iUsuario.findByEmail(req.email());
        if (opt.isEmpty() || !opt.get().getPasswordHash().equals(UsuarioController.sha256(req.password()))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email o password incorrectos");
        }
        return ResponseEntity.ok(UsuarioResponse.de(opt.get()));
    }

    // Ver mis datos (GET /auth/me?idUsuario=)
    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestParam Integer idUsuario) {
        return iUsuario.findById(idUsuario)
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(UsuarioResponse.de(u)))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no valido"));
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
