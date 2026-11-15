package com.uv.backend.controller;

import com.uv.backend.entity.Curso;
import com.uv.backend.entity.CursoAlumnoId;
import com.uv.backend.entity.Usuario;
import com.uv.backend.repository.CursoRepository;
import com.uv.backend.repository.ICursoAlumno;
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
    private final ICursoAlumno cursoAlumnoRepository;

    public CursoController(CursoRepository cursoRepository, UsuarioRepository usuarioRepository,
            ICursoAlumno cursoAlumnoRepository) {
        this.cursoRepository = cursoRepository;
        this.usuarioRepository = usuarioRepository;
        this.cursoAlumnoRepository = cursoAlumnoRepository;
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
        c.setMaestro(maestro.get());
        c.setCodigoInvitacion(generarCodigo());
        Curso guardado = cursoRepository.save(c);

        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    // Editar curso (PUT /cursos/{id}): solo los campos no null se actualizan
    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Integer id, @RequestBody CursoRequest req) {
        var opt = cursoRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Curso c = opt.get();
        if (req.nombre() != null && !req.nombre().isBlank()) {
            c.setNombre(req.nombre());
        }
        if (req.descripcion() != null) {
            c.setDescripcion(req.descripcion());
        }
        if (req.idMaestro() != null) {
            Optional<Usuario> maestro = usuarioRepository.findById(req.idMaestro());
            if (maestro.isEmpty() || !"MAESTRO".equals(maestro.get().getRol())) {
                return ResponseEntity.badRequest().body("idMaestro no corresponde a un usuario con rol MAESTRO");
            }
            c.setMaestro(maestro.get());
        }
        return ResponseEntity.ok(cursoRepository.save(c));
    }

    // Eliminar curso (DELETE /cursos/{id})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (!cursoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        cursoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Dar de baja alumno (DELETE /cursos/{id}/alumnos/{idAlumno})
    @DeleteMapping("/{id}/alumnos/{idAlumno}")
    public ResponseEntity<?> darDeBaja(@PathVariable Integer id, @PathVariable Integer idAlumno) {
        if (!cursoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        var clave = new CursoAlumnoId(id, idAlumno);
        if (!cursoAlumnoRepository.existsById(clave)) {
            return ResponseEntity.notFound().build();
        }
        cursoAlumnoRepository.deleteById(clave);
        return ResponseEntity.noContent().build();
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
