package com.uv.backend.controller;

import com.uv.backend.entity.Curso;
import com.uv.backend.entity.CursoAlumno;
import com.uv.backend.entity.CursoAlumnoId;
import com.uv.backend.repository.ICurso;
import com.uv.backend.service.AccesoCurso;
import com.uv.backend.service.Filtros;
import com.uv.backend.entity.Usuario;
import com.uv.backend.repository.ICursoAlumno;
import com.uv.backend.repository.IUsuario;
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

    private final IUsuario iUsuario;
    private final ICursoAlumno cursoAlumnoRepository;

    private final ICurso iCurso;
    private final AccesoCurso accesoCurso;

    public CursoController(IUsuario iUsuario,
            ICursoAlumno cursoAlumnoRepository, ICurso iCurso, AccesoCurso accesoCurso) {
        this.iUsuario = iUsuario;
        this.cursoAlumnoRepository = cursoAlumnoRepository;
        this.iCurso = iCurso;
        this.accesoCurso = accesoCurso;
    }

    // Datos para unirse a un curso
    public record CodigoRequest(String codigoInvitacion) {}

    // Datos que llegan en el POST
    public record CursoRequest(String nombre, String descripcion, Integer idMaestro) {}

    // Ver cursos (GET /cursos). Con ?idUsuario= devuelve solo "mis cursos":
    // MAESTRO -> los que imparte, ALUMNO -> en los que esta inscrito, ADMIN -> todos
    // Filtros: ?q= (nombre o descripcion), ?idMaestro=
    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) Integer idUsuario,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer idMaestro) {
        List<Curso> cursos;
        if (idUsuario == null) {
            cursos = iCurso.findAll();
        } else {
            Optional<Usuario> usuario = iUsuario.findById(idUsuario);
            if (usuario.isEmpty()) {
                return ResponseEntity.badRequest().body("El usuario no existe");
            }
            cursos = switch (usuario.get().getRol()) {
                case "MAESTRO" -> iCurso.findByMaestroIdUsuario(idUsuario);
                case "ALUMNO" -> cursoAlumnoRepository.findByAlumnoIdUsuario(idUsuario)
                        .stream().map(CursoAlumno::getCurso).toList();
                default -> iCurso.findAll();
            };
        }
        return ResponseEntity.ok(cursos.stream()
                .filter(c -> Filtros.contiene(q, c.getNombre(), c.getDescripcion()))
                .filter(c -> idMaestro == null || c.getMaestro().getIdUsuario().equals(idMaestro))
                .toList());
    }

    // Unirse con codigo de invitacion (POST /cursos/unirse?idUsuario=)
    @PostMapping("/unirse")
    public ResponseEntity<?> unirse(@RequestParam Integer idUsuario, @RequestBody CodigoRequest req) {
        if (req.codigoInvitacion() == null || req.codigoInvitacion().isBlank()) {
            return ResponseEntity.badRequest().body("codigoInvitacion es obligatorio");
        }
        Optional<Usuario> alumno = iUsuario.findById(idUsuario);
        if (alumno.isEmpty() || !"ALUMNO".equals(alumno.get().getRol())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Solo un ALUMNO puede unirse a un curso");
        }
        Optional<Curso> curso = iCurso.findByCodigoInvitacion(req.codigoInvitacion().trim().toUpperCase());
        if (curso.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Codigo de invitacion no valido");
        }
        if (cursoAlumnoRepository.existsById(new CursoAlumnoId(curso.get().getIdCurso(), idUsuario))) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya estas inscrito en este curso");
        }
        cursoAlumnoRepository.save(new CursoAlumno(curso.get(), alumno.get()));
        return ResponseEntity.status(HttpStatus.CREATED).body(curso.get());
    }

    // Generar nuevo codigo de invitacion (POST /cursos/{id}/codigo?idUsuario=)
    @PostMapping("/{id}/codigo")
    public ResponseEntity<?> regenerarCodigo(@PathVariable Integer id, @RequestParam Integer idUsuario) {
        Optional<Curso> opt = iCurso.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!accesoCurso.esMaestro(opt.get(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Solo el maestro del curso puede cambiar el codigo");
        }
        Curso c = opt.get();
        c.setCodigoInvitacion(generarCodigo());
        return ResponseEntity.ok(iCurso.save(c));
    }

    // Ver alumnos inscritos (GET /cursos/{id}/alumnos?idUsuario=)
    // Filtro: ?q= (nombre o email)
    @GetMapping("/{id}/alumnos")
    public ResponseEntity<?> alumnos(@PathVariable Integer id, @RequestParam Integer idUsuario,
            @RequestParam(required = false) String q) {
        Optional<Curso> opt = iCurso.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!accesoCurso.esMiembro(opt.get(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No perteneces a este curso");
        }
        return ResponseEntity.ok(cursoAlumnoRepository.findByCursoIdCurso(id)
                .stream().map(CursoAlumno::getAlumno)
                .filter(a -> Filtros.contiene(q, a.getNombre(), a.getEmail()))
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Curso> obtener(@PathVariable Integer id) {
        return iCurso.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody CursoRequest req) {
        if (req.nombre() == null || req.nombre().isBlank() || req.idMaestro() == null) {
            return ResponseEntity.badRequest().body("nombre e idMaestro son obligatorios");
        }
        Optional<Usuario> maestro = iUsuario.findById(req.idMaestro());
        if (maestro.isEmpty() || !"MAESTRO".equals(maestro.get().getRol())) {
            return ResponseEntity.badRequest().body("idMaestro no corresponde a un usuario con rol MAESTRO");
        }

        Curso c = new Curso();
        c.setNombre(req.nombre());
        c.setDescripcion(req.descripcion());
        c.setMaestro(maestro.get());
        c.setCodigoInvitacion(generarCodigo());
        Curso guardado = iCurso.save(c);

        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    // Editar curso (PUT /cursos/{id}): solo los campos no null se actualizan
    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Integer id, @RequestBody CursoRequest req) {
        var opt = iCurso.findById(id);
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
            Optional<Usuario> maestro = iUsuario.findById(req.idMaestro());
            if (maestro.isEmpty() || !"MAESTRO".equals(maestro.get().getRol())) {
                return ResponseEntity.badRequest().body("idMaestro no corresponde a un usuario con rol MAESTRO");
            }
            c.setMaestro(maestro.get());
        }
        return ResponseEntity.ok(iCurso.save(c));
    }

    // Eliminar curso (DELETE /cursos/{id})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (!iCurso.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        iCurso.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Dar de baja alumno (DELETE /cursos/{id}/alumnos/{idAlumno})
    @DeleteMapping("/{id}/alumnos/{idAlumno}")
    public ResponseEntity<?> darDeBaja(@PathVariable Integer id, @PathVariable Integer idAlumno) {
        if (!iCurso.existsById(id)) {
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
        } while (iCurso.existsByCodigoInvitacion(codigo));
        return codigo;
    }
}
