package com.uv.backend.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import com.uv.backend.entity.Tarea;
import com.uv.backend.entity.Usuario;
import com.uv.backend.repository.ITarea;
import com.uv.backend.service.AccesoCurso;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uv.backend.entity.Equipo;
import com.uv.backend.repository.IEquipo;
import com.uv.backend.repository.IUsuario;

@RestController 
@RequestMapping("/api")
public class EquipoController {

    @Autowired 
    IEquipo iEquipo;

    @Autowired
    IUsuario iUsuario;

    //metodo para validar permisos de modificacion
    private boolean tienePermiso(Equipo equipo, Integer idUsuario) {
        boolean esMaestroCurso = equipo.getTarea() != null &&
                equipo.getTarea().getCurso().getMaestro().getIdUsuario().equals(idUsuario);

        boolean esIntegrante = equipo.getIntegrantes() != null &&
                equipo.getIntegrantes().stream()
                        .anyMatch(alumno -> alumno.getIdUsuario().equals(idUsuario));

        return esMaestroCurso || esIntegrante;
    }

    // Editar equipo
    @PutMapping("/equipos/{id}")
    public ResponseEntity<?> editarEquipo(@PathVariable Integer id,
        @RequestParam Integer idUsuario,
        @RequestBody Equipo equipoNuevo) {
        if (!iEquipo.existsById(id) || !iUsuario.existsById(idUsuario)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El equipo no existe");
        }

        Equipo equipoBase = iEquipo.findById(id).get();

        if (!tienePermiso(equipoBase, idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Usuario no autorizado");
        }

        if (equipoNuevo.getNombreEquipo() != null) {
            equipoBase.setNombreEquipo(equipoNuevo.getNombreEquipo());
        }
        if (equipoNuevo.getIntegrantes() != null) {
            equipoBase.setIntegrantes(equipoNuevo.getIntegrantes());
        }

        iEquipo.save(equipoBase);
        return ResponseEntity.ok(equipoBase);
    }

    // Eliminar equipo
    @DeleteMapping("/equipos/{id}")
    public ResponseEntity<?> eliminarEquipo(@PathVariable Integer id,
        @RequestParam Integer idUsuario) {
        if (!iEquipo.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El equipo no existe");
        }
        if (!iUsuario.existsById(idUsuario)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El usuario no existe");
        }

        Equipo equipoBase = iEquipo.findById(id).get();

        if (!tienePermiso(equipoBase, idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Usuario no autorizado");
        }

        iEquipo.deleteById(id);
        return ResponseEntity.ok("Equipo eliminado");
    }

    // Eliminar integrantes del equipo
    @DeleteMapping("/equipos/{id}/integrantes/{idAlumno}")
    public ResponseEntity<?> eliminarIntegrante(@PathVariable Integer id, 
        @PathVariable Integer idAlumno,
        @RequestParam Integer idUsuario) {
        if (!iEquipo.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El equipo no existe");
        }
        if (!iUsuario.existsById(idUsuario)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El usuario no existe");
        }

        Equipo equipo = iEquipo.findById(id).get();

        if (!tienePermiso(equipo, idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Usuario no autorizado");
        }

        boolean eliminado = equipo.getIntegrantes().removeIf(alumno -> alumno.getIdUsuario().equals(idAlumno));
        if (!eliminado) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El alumno no pertenece a este equipo");
        }

        iEquipo.save(equipo);
        return ResponseEntity.ok("Integrante eliminado");
    }

    // ===== GET y POST =====

    @Autowired
    ITarea iTarea;
    @Autowired
    AccesoCurso accesoCurso;

    public record EquipoRequest(String nombreEquipo) {}

    public record IntegrantesRequest(List<Integer> idsAlumnos) {}

    //ver equipos de una tarea
    @GetMapping ("/tareas/{idTarea}/equipos")
    public ResponseEntity<?> listarEquipos(@PathVariable Integer idTarea, @RequestParam Integer idUsuario) {
        Optional<Tarea> tarea = iTarea.findById(idTarea);
        if (tarea.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("La tarea no existe");
        }
        if (!accesoCurso.esMiembro(tarea.get().getCurso(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No perteneces a este curso");
        }
        return ResponseEntity.ok(iEquipo.findByTareaIdTarea(idTarea));
    }

    //crear equipo
    @PostMapping ("/tareas/{idTarea}/equipos")
    public ResponseEntity<?> crearEquipo(@PathVariable Integer idTarea, @RequestParam Integer idUsuario,
            @RequestBody EquipoRequest req) {
        Optional<Tarea> tarea = iTarea.findById(idTarea);
        if (tarea.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("La tarea no existe");
        }
        // Crear un equipo lo puede hacer el maestro del curso o un alumno
        // inscrito en el curso.
        boolean esMaestro = accesoCurso.esMaestro(tarea.get().getCurso(), idUsuario);
        boolean esAlumno = accesoCurso.esAlumnoInscrito(tarea.get().getCurso(), idUsuario);
        if (!esMaestro && !esAlumno) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No perteneces a este curso");
        }
        if (!Boolean.TRUE.equals(tarea.get().getEsEquipo())) {
            return ResponseEntity.badRequest().body("La tarea no es por equipo");
        }
        if (req.nombreEquipo() == null || req.nombreEquipo().isBlank()) {
            return ResponseEntity.badRequest().body("nombreEquipo es obligatorio");
        }
        // Un alumno solo puede estar en un equipo por tarea.
        if (!esMaestro) {
            boolean yaEnEquipo = iEquipo.findByTareaIdTarea(tarea.get().getIdTarea()).stream()
                    .anyMatch(e -> e.getIntegrantes().stream().anyMatch(u -> u.getIdUsuario().equals(idUsuario)));
            if (yaEnEquipo) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya perteneces a un equipo de esta tarea");
            }
        }
        Equipo equipo = iEquipo.save(new Equipo(tarea.get(), req.nombreEquipo()));
        // Si lo crea un alumno, queda como integrante de su propio equipo.
        if (!esMaestro) {
            Usuario creador = iUsuario.findById(idUsuario).get();
            equipo.getIntegrantes().add(creador);
            equipo = iEquipo.save(equipo);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(equipo);
    }

    //ver equipo
    @GetMapping ("/equipos/{id}")
    public ResponseEntity<?> obtenerEquipo(@PathVariable Integer id, @RequestParam Integer idUsuario) {
        Optional<Equipo> equipo = iEquipo.findById(id);
        if (equipo.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El equipo no existe");
        }
        if (!accesoCurso.esMiembro(equipo.get().getTarea().getCurso(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No perteneces a este curso");
        }
        return ResponseEntity.ok(equipo.get());
    }

    //agregar integrantes
    @PostMapping ("/equipos/{id}/integrantes")
    public ResponseEntity<?> agregarIntegrantes(@PathVariable Integer id, @RequestParam Integer idUsuario,
            @RequestBody IntegrantesRequest req) {
        Optional<Equipo> opt = iEquipo.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El equipo no existe");
        }
        Equipo equipo = opt.get();
        if (req.idsAlumnos() == null || req.idsAlumnos().isEmpty()) {
            return ResponseEntity.badRequest().body("idsAlumnos es obligatorio");
        }
        // Agregar integrantes al equipo
        boolean esMaestro = accesoCurso.esMaestro(equipo.getTarea().getCurso(), idUsuario);
        boolean soyIntegrante = equipo.getIntegrantes().stream()
                .anyMatch(u -> u.getIdUsuario().equals(idUsuario));
        boolean soloMeAgrego = req.idsAlumnos().size() == 1
                && req.idsAlumnos().get(0).equals(idUsuario);
        if (!esMaestro && !soyIntegrante && !soloMeAgrego) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Solo el maestro del curso o un integrante del equipo puede agregar integrantes");
        }
        List<Equipo> equiposDeLaTarea = iEquipo.findByTareaIdTarea(equipo.getTarea().getIdTarea());
        for (Integer idAlumno : req.idsAlumnos()) {
            if (!accesoCurso.esAlumnoInscrito(equipo.getTarea().getCurso(), idAlumno)) {
                return ResponseEntity.badRequest().body("El alumno " + idAlumno + " no esta inscrito en el curso");
            }
            boolean yaEnEsteEquipo = equipo.getIntegrantes().stream()
                    .anyMatch(u -> u.getIdUsuario().equals(idAlumno));
            if (yaEnEsteEquipo) {
                continue;
            }
            boolean enOtroEquipo = equiposDeLaTarea.stream()
                    .filter(e -> !e.getIdEquipo().equals(equipo.getIdEquipo()))
                    .anyMatch(e -> e.getIntegrantes().stream().anyMatch(u -> u.getIdUsuario().equals(idAlumno)));
            if (enOtroEquipo) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("El alumno " + idAlumno + " ya pertenece a otro equipo de esta tarea");
            }
            Usuario alumno = iUsuario.findById(idAlumno).get();
            equipo.getIntegrantes().add(alumno);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(iEquipo.save(equipo));
    }
}
