package com.uv.backend.controller;

import com.uv.backend.model.Curso;
import com.uv.backend.model.Usuario;
import com.uv.backend.repository.CursoRepository;
import com.uv.backend.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/cursos")
public class CursoController {

    private static final String CARACTERES = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CursoRepository cursoRepository;
    private final UsuarioRepository usuarioRepository;

    public CursoController(CursoRepository cursoRepository, UsuarioRepository usuarioRepository) {
        this.cursoRepository = cursoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // Datos que llegan en el POST
    public record CursoRequest(String nombre, String descripcion, Integer idMaestro) {}

    @GetMapping
    public List<Curso> listar() {
        return cursoRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Curso> obtener(@PathVariable Integer id) {
        return cursoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody CursoRequest req) {
        if (req.nombre() == null || req.nombre().isBlank() || req.idMaestro() == null) {
            return ResponseEntity.badRequest().body("nombre e idMaestro son obligatorios");
        }
        Optional<Usuario> maestro = usuarioRepository.findById(req.idMaestro());
        if (maestro.isEmpty() || !"MAESTRO".equals(maestro.get().getRol())) {
            return ResponseEntity.badRequest().body("idMaestro no corresponde a un usuario con rol MAESTRO");
        }

        Curso c = new Curso();
        c.setNombre(req.nombre());
        c.setDescripcion(req.descripcion());
        c.setIdMaestro(req.idMaestro());
        c.setCodigoInvitacion(generarCodigo());
        Curso guardado = cursoRepository.save(c);

        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    // Genera un codigo de invitacion de 6 caracteres que no exista ya
    private String generarCodigo() {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(CARACTERES.charAt(RANDOM.nextInt(CARACTERES.length())));
            }
            codigo = sb.toString();
        } while (cursoRepository.existsByCodigoInvitacion(codigo));
        return codigo;
    }
}
