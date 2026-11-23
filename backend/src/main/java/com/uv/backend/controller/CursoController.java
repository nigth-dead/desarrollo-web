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
import com.uv.backend.security.AuthInterceptor;
import com.uv.backend.security.Permisos;
import com.uv.backend.security.TokenService.DatosToken;
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
    private final Permisos permisos;

    private static final String SESION = AuthInterceptor.ATRIBUTO;

    public CursoController(IUsuario iUsuario,
            ICursoAlumno cursoAlumnoRepository, ICurso iCurso, AccesoCurso accesoCurso, Permisos permisos) {
        this.iUsuario = iUsuario;
        this.cursoAlumnoRepository = cursoAlumnoRepository;
        this.iCurso = iCurso;
        this.accesoCurso = accesoCurso;
        this.permisos = permisos;
    }

    // Datos para unirse a un curso
    public record CodigoRequest(String codigoInvitacion) {}

    // Datos que llegan en el POST
    public record CursoRequest(String nombre, String descripcion, Integer idMaestro) {}

    // Datos que llegan en el POST /cursos/{id}/alumnos
    public record InscribirRequest(String email) {}

    // Ver cursos (GET /cursos). Con ?idUsuario= devuelve solo "mis cursos":
    // MAESTRO -> los que imparte, ALUMNO -> en los que esta inscrito, ADMIN -> todos
    // Filtros: ?q= (nombre o descripcion), ?idMaestro=
    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) Integer idUsuario,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer idMaestro,
            @RequestAttribute(name = SESION, required = false) DatosToken sesion) {
        // sin ?idUsuario= un usuario que no es ADMIN ve solo sus cursos (no los codigos de todos)
        if (idUsuario == null && sesion != null && !permisos.esAdmin(sesion)) {
            idUsuario = sesion.idUsuario();
        }
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
        // se regresa la inscripcion (alumno + fechaInscripcion), no solo el alumno
        return ResponseEntity.ok(cursoAlumnoRepository.findByCursoIdCurso(id).stream()
                .filter(ca -> Filtros.contiene(q, ca.getAlumno().getNombre(), ca.getAlumno().getEmail()))
                .toList());
    }

    // Inscribir alumno por correo (POST /cursos/{id}/alumnos?idUsuario=)
    // Solo el maestro del curso. El alumno ya debe tener cuenta registrada.
    @PostMapping("/{id}/alumnos")
    public ResponseEntity<?> inscribirAlumno(@PathVariable Integer id, @RequestBody InscribirRequest req,
            @RequestParam Integer idUsuario) {
        Optional<Curso> opt = iCurso.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El curso no existe");
        }
        if (!accesoCurso.esMaestro(opt.get(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Solo el maestro del curso puede inscribir alumnos");
        }
        if (req.email() == null || req.email().isBlank()) {
            return ResponseEntity.badRequest().body("email es obligatorio");
        }
        Optional<Usuario> alumno = iUsuario.findByEmail(req.email().trim());
        if (alumno.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No existe una cuenta con ese correo.");
        }
        if (!"ALUMNO".equals(alumno.get().getRol())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("La cuenta no es de un alumno.");
        }
        if (cursoAlumnoRepository.existsById(new CursoAlumnoId(id, alumno.get().getIdUsuario()))) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("El alumno ya está inscrito en este curso");
        }
        CursoAlumno guardado = cursoAlumnoRepository.save(new CursoAlumno(opt.get(), alumno.get()));
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    // Ver curso: solo sus miembros o un ADMIN (incluye el codigo de invitacion)
    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Integer id,
            @RequestAttribute(name = SESION, required = false) DatosToken sesion) {
        Optional<Curso> curso = iCurso.findById(id);
        if (curso.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (sesion != null && !permisos.esAdmin(sesion) && !accesoCurso.esMiembro(curso.get(), sesion.idUsuario())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No perteneces a este curso");
        }
        return ResponseEntity.ok(curso.get());
    }

    // Crear curso: un MAESTRO para si mismo, o un ADMIN para cualquier maestro
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody CursoRequest req,
            @RequestAttribute(name = SESION, required = false) DatosToken sesion) {
        if (req.nombre() == null || req.nombre().isBlank() || req.idMaestro() == null) {
            return ResponseEntity.badRequest().body("nombre e idMaestro son obligatorios");
        }
        if (!permisos.esAdmin(sesion) && !permisos.esElMismo(sesion, req.idMaestro())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Solo puedes crear cursos a tu nombre");
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

    // Editar curso (PUT /cursos/{id}): maestro del curso o ADMIN. Solo los campos no null se actualizan
    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Integer id, @RequestBody CursoRequest req,
            @RequestAttribute(name = SESION, required = false) DatosToken sesion) {
        var opt = iCurso.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Curso c = opt.get();
        if (!esMaestroOAdmin(c, sesion)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Solo el maestro del curso puede editarlo");
        }
        if (req.idMaestro() != null && !req.idMaestro().equals(c.getMaestro().getIdUsuario())
                && !permisos.esAdmin(sesion)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Solo el administrador puede cambiar el maestro del curso");
        }
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

    // Eliminar curso (DELETE /cursos/{id}): maestro del curso o ADMIN
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id,
            @RequestAttribute(name = SESION, required = false) DatosToken sesion) {
        Optional<Curso> curso = iCurso.findById(id);
        if (curso.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!esMaestroOAdmin(curso.get(), sesion)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Solo el maestro del curso puede eliminarlo");
        }
        iCurso.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Dar de baja alumno (DELETE /cursos/{id}/alumnos/{idAlumno}): maestro, ADMIN o el propio alumno
    @DeleteMapping("/{id}/alumnos/{idAlumno}")
    public ResponseEntity<?> darDeBaja(@PathVariable Integer id, @PathVariable Integer idAlumno,
            @RequestAttribute(name = SESION, required = false) DatosToken sesion) {
        Optional<Curso> curso = iCurso.findById(id);
        if (curso.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!esMaestroOAdmin(curso.get(), sesion) && !permisos.esElMismo(sesion, idAlumno)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Solo el maestro del curso puede dar de baja a otros alumnos");
        }
        var clave = new CursoAlumnoId(id, idAlumno);
        if (!cursoAlumnoRepository.existsById(clave)) {
            return ResponseEntity.notFound().build();
        }
        cursoAlumnoRepository.deleteById(clave);
        return ResponseEntity.noContent().build();
    }

    private boolean esMaestroOAdmin(Curso curso, DatosToken sesion) {
        return sesion == null || accesoCurso.esMaestro(curso, sesion.idUsuario()) || permisos.esAdmin(sesion);
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
