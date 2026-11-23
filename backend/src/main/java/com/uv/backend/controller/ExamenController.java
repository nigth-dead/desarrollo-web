package com.uv.backend.controller;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import com.uv.backend.entity.Curso;
import com.uv.backend.repository.ICurso;
import com.uv.backend.service.AccesoCurso;
import com.uv.backend.service.Filtros;
import java.util.Comparator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uv.backend.entity.Examen;
import com.uv.backend.repository.IExamen;
import com.uv.backend.repository.IUsuario;

@RestController 
@RequestMapping ("/api")
public class ExamenController {
    @Autowired
    IExamen iExamen;
    @Autowired
    IUsuario iUsuario;

    //metodo para validar permisos de modificacion
    private boolean tienePermiso(Examen examen, Integer idUsuario) {
        return examen.getCurso() != null &&
                examen.getCurso().getMaestro() != null &&
                examen.getCurso().getMaestro().getIdUsuario().equals(idUsuario);
    }

    //editar examen
    @PutMapping ("/examenes/{id}")
    public ResponseEntity<?> editarExamen(@PathVariable Integer id,
        @RequestParam Integer idUsuario,
        @RequestBody Examen examenNuevo) {
        if (!iExamen.existsById(id) || !iUsuario.existsById(idUsuario)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El examen no existe");
        }
        Examen examenBase = iExamen.findById(id).get();
        if (!tienePermiso(examenBase, idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Usuario no autorizado");
        }
        if (examenNuevo.getTitulo() != null) {
            examenBase.setTitulo(examenNuevo.getTitulo());
        }
        if (examenNuevo.getFechaInicio() != null) {
            examenBase.setFechaInicio(examenNuevo.getFechaInicio());
        }
        if (examenNuevo.getFechaFin() != null) {
            examenBase.setFechaFin(examenNuevo.getFechaFin());
        }
        iExamen.save(examenBase);
        return ResponseEntity.ok(examenBase);
    }

    //eliminar examen
    @DeleteMapping ("/examenes/{id}")
    public ResponseEntity<?> eliminarExamen(@PathVariable Integer id,
        @RequestParam Integer idUsuario) {
        if (!iExamen.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El examen no existe");
        }
        if (!iUsuario.existsById(idUsuario)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El usuario no existe");
        }
        Examen examenBase = iExamen.findById(id).get();
        if (!tienePermiso(examenBase, idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Usuario no autorizado");
        }
        iExamen.deleteById(id);
        return ResponseEntity.ok("Examen eliminado");
    }

    // ===== GET y POST =====

    @Autowired
    ICurso iCurso;
    @Autowired
    AccesoCurso accesoCurso;

    public record ExamenRequest(String titulo, LocalDateTime fechaInicio, LocalDateTime fechaFin) {}

    //ver examenes del curso
    //filtros: ?estado=proximo|disponible|finalizado, ?q= (titulo); ordenados por fecha de inicio
    @GetMapping ("/cursos/{idCurso}/examenes")
    public ResponseEntity<?> listarExamenes(@PathVariable Integer idCurso, @RequestParam Integer idUsuario,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String q) {
        if (!Filtros.valorValido(estado, "proximo", "disponible", "finalizado")) {
            return ResponseEntity.badRequest().body("estado debe ser proximo, disponible o finalizado");
        }
        Optional<Curso> curso = iCurso.findById(idCurso);
        if (curso.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El curso no existe");
        }
        if (!accesoCurso.esMiembro(curso.get(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No perteneces a este curso");
        }
        LocalDateTime ahora = LocalDateTime.now();
        return ResponseEntity.ok(iExamen.findByCursoIdCurso(idCurso).stream()
                .filter(e -> Filtros.contiene(q, e.getTitulo()))
                .filter(e -> {
                    if (estado == null) {
                        return true;
                    }
                    String actual = ahora.isBefore(e.getFechaInicio()) ? "proximo"
                            : ahora.isAfter(e.getFechaFin()) ? "finalizado" : "disponible";
                    return actual.equalsIgnoreCase(estado);
                })
                .sorted(Comparator.comparing(Examen::getFechaInicio))
                .toList());
    }

    //crear examen
    @PostMapping ("/cursos/{idCurso}/examenes")
    public ResponseEntity<?> crearExamen(@PathVariable Integer idCurso, @RequestParam Integer idUsuario,
            @RequestBody ExamenRequest req) {
        Optional<Curso> curso = iCurso.findById(idCurso);
        if (curso.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El curso no existe");
        }
        if (!accesoCurso.esMaestro(curso.get(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Solo el maestro del curso puede crear examenes");
        }
        if (req.titulo() == null || req.titulo().isBlank() || req.fechaInicio() == null || req.fechaFin() == null) {
            return ResponseEntity.badRequest().body("titulo, fechaInicio y fechaFin son obligatorios");
        }
        if (!req.fechaFin().isAfter(req.fechaInicio())) {
            return ResponseEntity.badRequest().body("fechaFin debe ser posterior a fechaInicio");
        }
        Examen examen = iExamen.save(new Examen(curso.get(), req.titulo(), req.fechaInicio(), req.fechaFin()));
        return ResponseEntity.status(HttpStatus.CREATED).body(examen);
    }

    //ver examen
    @GetMapping ("/examenes/{id}")
    public ResponseEntity<?> obtenerExamen(@PathVariable Integer id, @RequestParam Integer idUsuario) {
        Optional<Examen> examen = iExamen.findById(id);
        if (examen.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El examen no existe");
        }
        if (!accesoCurso.esMiembro(examen.get().getCurso(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No perteneces a este curso");
        }
        return ResponseEntity.ok(examen.get());
    }
}
