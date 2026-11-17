package com.uv.backend.controller;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import com.uv.backend.entity.Curso;
import com.uv.backend.entity.CursoAlumno;
import com.uv.backend.entity.Notificacion;
import com.uv.backend.repository.ICurso;
import com.uv.backend.repository.ICursoAlumno;
import com.uv.backend.repository.INotificacion;
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

import com.uv.backend.entity.Tarea;
import com.uv.backend.repository.ITarea;
import com.uv.backend.repository.IUsuario;

@RestController 
@RequestMapping ("/api")
public class TareaController {
    @Autowired 
    ITarea iTarea;
    @Autowired
    IUsuario iUsuario;

    //metodo para validar permisos de modificacion
    //Solo el maestro del curso al que pertenece la tarea puede editarla o eliminarla
    private boolean tienePermiso(Tarea tarea, Integer idUsuario) {
        return tarea.getCurso() != null &&
                tarea.getCurso().getMaestro() != null &&
                tarea.getCurso().getMaestro().getIdUsuario().equals(idUsuario);
    }

    //editar tarea
    @PutMapping ("/tareas/{id}")
    public Tarea editarTarea(@PathVariable Integer id,
        @RequestParam Integer idUsuario,
        @RequestBody Tarea tareaNueva){
        if (!iTarea.existsById(id) || !iUsuario.existsById(idUsuario)) {
            return null;
        }

        Tarea tareaBase = iTarea.findById(id).get();

        if (!tienePermiso(tareaBase, idUsuario)) {
            return null;
        }

        //reemplazar contenido de la tarea
        if(tareaNueva.getTitulo() != null){
            tareaBase.setTitulo(tareaNueva.getTitulo());
        }
        if(tareaNueva.getDescripcion() != null){
            tareaBase.setDescripcion(tareaNueva.getDescripcion());
        }
        if(tareaNueva.getFechaProgramada() != null){
            tareaBase.setFechaProgramada(tareaNueva.getFechaProgramada());
        }
        if(tareaNueva.getFechaLimite() != null){
            tareaBase.setFechaLimite(tareaNueva.getFechaLimite());
        }
        if (tareaNueva.getEsEquipo() != null) {
            tareaBase.setEsEquipo(tareaNueva.getEsEquipo());
        }

        iTarea.save(tareaBase);

        return tareaBase;
    }

    //eliminar tarea
    @DeleteMapping ("/tareas/{id}")
    public String eliminarTarea(@PathVariable Integer id,
        @RequestParam Integer idUsuario) {
        if(!iTarea.existsById(id)){
            return "La tarea no existe";
        }
        if (!iUsuario.existsById(idUsuario)) {
            return "El usuario no existe";
        }

        Tarea tareaBase = iTarea.findById(id).get();

        if (!tienePermiso(tareaBase, idUsuario)) {
            return "Usuario no autorizado";
        }

        iTarea.deleteById(id);
        return "Tarea eliminada";
    }

    // ===== GET y POST =====

    @Autowired
    ICurso iCurso;
    @Autowired
    ICursoAlumno iCursoAlumno;
    @Autowired
    INotificacion iNotificacion;
    @Autowired
    AccesoCurso accesoCurso;

    public record TareaRequest(String titulo, String descripcion, LocalDateTime fechaProgramada,
            LocalDateTime fechaLimite, Boolean esEquipo) {}

    //ver tareas del curso (el alumno solo ve las que ya pasaron su fecha programada)
    //filtros: ?q= (titulo o descripcion), ?estado=pendiente|vencida, ?esEquipo=true|false
    //ordenadas por fecha limite (las que no tienen fecha al final)
    @GetMapping ("/cursos/{idCurso}/tareas")
    public ResponseEntity<?> listarTareas(@PathVariable Integer idCurso, @RequestParam Integer idUsuario,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Boolean esEquipo) {
        if (!Filtros.valorValido(estado, "pendiente", "vencida")) {
            return ResponseEntity.badRequest().body("estado debe ser pendiente o vencida");
        }
        Optional<Curso> curso = iCurso.findById(idCurso);
        if (curso.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El curso no existe");
        }
        if (!accesoCurso.esMiembro(curso.get(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No perteneces a este curso");
        }
        boolean esMaestro = accesoCurso.esMaestro(curso.get(), idUsuario);
        LocalDateTime ahora = LocalDateTime.now();
        return ResponseEntity.ok(iTarea.findByCursoIdCurso(idCurso).stream()
                .filter(t -> esMaestro || t.getFechaProgramada() == null || !t.getFechaProgramada().isAfter(ahora))
                .filter(t -> Filtros.contiene(q, t.getTitulo(), t.getDescripcion()))
                .filter(t -> esEquipo == null || esEquipo.equals(Boolean.TRUE.equals(t.getEsEquipo())))
                .filter(t -> {
                    if (estado == null) {
                        return true;
                    }
                    boolean vencida = t.getFechaLimite() != null && t.getFechaLimite().isBefore(ahora);
                    return estado.equalsIgnoreCase("vencida") == vencida;
                })
                .sorted(Comparator.comparing(Tarea::getFechaLimite, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList());
    }

    //crear tarea
    @PostMapping ("/cursos/{idCurso}/tareas")
    public ResponseEntity<?> crearTarea(@PathVariable Integer idCurso, @RequestParam Integer idUsuario,
            @RequestBody TareaRequest req) {
        Optional<Curso> curso = iCurso.findById(idCurso);
        if (curso.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El curso no existe");
        }
        if (!accesoCurso.esMaestro(curso.get(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Solo el maestro del curso puede crear tareas");
        }
        if (req.titulo() == null || req.titulo().isBlank()) {
            return ResponseEntity.badRequest().body("titulo es obligatorio");
        }
        if (req.fechaProgramada() != null && req.fechaLimite() != null
                && req.fechaLimite().isBefore(req.fechaProgramada())) {
            return ResponseEntity.badRequest().body("fechaLimite no puede ser anterior a fechaProgramada");
        }
        Tarea t = new Tarea();
        t.setCurso(curso.get());
        t.setTitulo(req.titulo());
        t.setDescripcion(req.descripcion());
        t.setFechaProgramada(req.fechaProgramada());
        t.setFechaLimite(req.fechaLimite());
        t.setEsEquipo(Boolean.TRUE.equals(req.esEquipo()));
        Tarea guardada = iTarea.save(t);

        //avisar a los alumnos del curso
        for (CursoAlumno ca : iCursoAlumno.findByCursoIdCurso(idCurso)) {
            Notificacion n = new Notificacion(ca.getAlumno(),
                    "Nueva tarea en " + curso.get().getNombre() + ": " + guardada.getTitulo());
            n.setLeido(false);
            iNotificacion.save(n);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
    }

    //ver tarea
    @GetMapping ("/tareas/{id}")
    public ResponseEntity<?> obtenerTarea(@PathVariable Integer id, @RequestParam Integer idUsuario) {
        Optional<Tarea> tarea = iTarea.findById(id);
        if (tarea.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("La tarea no existe");
        }
        if (!accesoCurso.esMiembro(tarea.get().getCurso(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No perteneces a este curso");
        }
        return ResponseEntity.ok(tarea.get());
    }
}
