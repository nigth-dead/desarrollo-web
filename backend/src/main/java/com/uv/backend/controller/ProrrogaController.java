package com.uv.backend.controller;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import com.uv.backend.entity.Equipo;
import com.uv.backend.entity.Tarea;
import com.uv.backend.entity.Usuario;
import com.uv.backend.repository.IEquipo;
import com.uv.backend.repository.ITarea;
import com.uv.backend.service.AccesoCurso;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uv.backend.entity.Prorroga;
import com.uv.backend.repository.IProrroga;
import com.uv.backend.repository.IUsuario;

@RestController 
@RequestMapping ("/api")
public class ProrrogaController {
    @Autowired 
    IProrroga iProrroga;
    @Autowired 
    IUsuario iUsuario;

    //metodo para validar permisos de modificacion
    //El solicitante es el alumno dueño o un integrante del equipo que la pidió
    private boolean esSolicitante(Prorroga prorroga, Integer idUsuario) {
        boolean esAlumnoDueno = prorroga.getAlumno() != null &&
                prorroga.getAlumno().getIdUsuario().equals(idUsuario);

        boolean esIntegranteEquipo = prorroga.getEquipo() != null &&
                prorroga.getEquipo().getIntegrantes() != null &&
                prorroga.getEquipo().getIntegrantes().stream()
                        .anyMatch(alumno -> alumno.getIdUsuario().equals(idUsuario));

        return esAlumnoDueno || esIntegranteEquipo;
    }

    //Solo el maestro del curso al que pertenece la tarea
    private boolean esMaestroCurso(Prorroga prorroga, Integer idUsuario) {
        return prorroga.getTarea() != null &&
                prorroga.getTarea().getCurso() != null &&
                prorroga.getTarea().getCurso().getMaestro() != null &&
                prorroga.getTarea().getCurso().getMaestro().getIdUsuario().equals(idUsuario);
    }

    //Aprobar/rechazar la puede el maestro del curso; eliminarla el solicitante o el maestro
    private boolean tienePermiso(Prorroga prorroga, Integer idUsuario) {
        return esSolicitante(prorroga, idUsuario) || esMaestroCurso(prorroga, idUsuario);
    }

    //aprobar o rechazar prorrogas
    @PatchMapping ("/prorrogas/{id}")
    public String actualizarProrroga(@PathVariable Integer id, 
        @RequestParam Integer idUsuario,
        @RequestBody Prorroga prorrogaNueva){
        if(!iProrroga.existsById(id)){
            return "La prorroga no existe";
        }
        if (!iUsuario.existsById(idUsuario)) {
            return "El usuario no existe";
        }

        Prorroga prorrogaBase = iProrroga.findById(id).get();

        if (!esMaestroCurso(prorrogaBase, idUsuario)) {
            return "Solo el maestro del curso puede aprobar o rechazar prorrogas";
        }
        if(prorrogaNueva.getAprobada() == null){
            return "Informacion incompleta";
        }

        if(Boolean.TRUE.equals(prorrogaNueva.getAprobada())){
            if(prorrogaNueva.getNuevaFechaLimite() == null){
                return "Informacion incompleta";
            }
            prorrogaBase.setAprobada(true);
            prorrogaBase.setNuevaFechaLimite(prorrogaNueva.getNuevaFechaLimite());
            iProrroga.save(prorrogaBase);
            return "Prorroga aprobada";
        }
        prorrogaBase.setAprobada(false);
        iProrroga.save(prorrogaBase);
        return "Prorroga rechazada";
    }

    //eliminar prorroga
    @DeleteMapping ("/prorrogas/{id}")
    public String eliminarProrroga(@PathVariable Integer id,
        @RequestParam Integer idUsuario) {
        if(!iProrroga.existsById(id)){
            return "Prorroga no existente";
        }
        if(!iUsuario.existsById(idUsuario)){
            return "El usuario no existe";
        }

        Prorroga prorrogaBase = iProrroga.findById(id).get();

        if (!tienePermiso(prorrogaBase, idUsuario)) {
            return "Usuario no autorizado";
        }

        iProrroga.deleteById(id);
        return "Prorroga eliminada";
    }

    // ===== GET y POST =====

    @Autowired
    ITarea iTarea;
    @Autowired
    IEquipo iEquipo;
    @Autowired
    AccesoCurso accesoCurso;

    //idAlumno / idEquipo solo los usa el maestro para otorgar una prorroga directa
    public record ProrrogaRequest(LocalDateTime nuevaFechaLimite, Integer idAlumno, Integer idEquipo) {}

    //ver solicitudes de prorroga (maestro)
    //filtro: ?aprobada=true|false (false = pendientes de revisar)
    @GetMapping ("/tareas/{idTarea}/prorrogas")
    public ResponseEntity<?> listarProrrogas(@PathVariable Integer idTarea, @RequestParam Integer idUsuario,
            @RequestParam(required = false) Boolean aprobada) {
        Optional<Tarea> tarea = iTarea.findById(idTarea);
        if (tarea.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("La tarea no existe");
        }
        if (!accesoCurso.esMaestro(tarea.get().getCurso(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Solo el maestro del curso puede ver las prorrogas");
        }
        return ResponseEntity.ok(iProrroga.findByTareaIdTarea(idTarea).stream()
                .filter(p -> aprobada == null || aprobada.equals(Boolean.TRUE.equals(p.getAprobada())))
                .toList());
    }

    //solicitar (alumno) u otorgar (maestro) prorroga
    @PostMapping ("/tareas/{idTarea}/prorrogas")
    public ResponseEntity<?> crearProrroga(@PathVariable Integer idTarea, @RequestParam Integer idUsuario,
            @RequestBody ProrrogaRequest req) {
        Optional<Tarea> opt = iTarea.findById(idTarea);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("La tarea no existe");
        }
        Tarea tarea = opt.get();
        if (req.nuevaFechaLimite() == null) {
            return ResponseEntity.badRequest().body("nuevaFechaLimite es obligatoria");
        }
        if (tarea.getFechaLimite() != null && !req.nuevaFechaLimite().isAfter(tarea.getFechaLimite())) {
            return ResponseEntity.badRequest().body("nuevaFechaLimite debe ser posterior a la fecha limite de la tarea");
        }
        boolean esEquipo = Boolean.TRUE.equals(tarea.getEsEquipo());

        //maestro: otorga la prorroga directamente (ya aprobada)
        if (accesoCurso.esMaestro(tarea.getCurso(), idUsuario)) {
            Usuario alumno = null;
            Equipo equipo = null;
            if (esEquipo) {
                Optional<Equipo> eq = req.idEquipo() == null ? Optional.empty() : iEquipo.findById(req.idEquipo());
                if (eq.isEmpty() || !eq.get().getTarea().getIdTarea().equals(idTarea)) {
                    return ResponseEntity.badRequest().body("idEquipo no valido para esta tarea");
                }
                equipo = eq.get();
            } else {
                if (req.idAlumno() == null || !accesoCurso.esAlumnoInscrito(tarea.getCurso(), req.idAlumno())) {
                    return ResponseEntity.badRequest().body("idAlumno no valido para este curso");
                }
                alumno = iUsuario.findById(req.idAlumno()).get();
            }
            Prorroga p = new Prorroga(tarea, alumno, equipo, req.nuevaFechaLimite());
            p.setAprobada(true);
            return ResponseEntity.status(HttpStatus.CREATED).body(iProrroga.save(p));
        }

        //alumno: solicita para si mismo o para su equipo (queda pendiente)
        if (!accesoCurso.esAlumnoInscrito(tarea.getCurso(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No perteneces a este curso");
        }
        Usuario alumno = null;
        Equipo equipo = null;
        if (esEquipo) {
            Optional<Equipo> eq = iEquipo.findByTareaIdTarea(idTarea).stream()
                    .filter(e -> e.getIntegrantes().stream().anyMatch(u -> u.getIdUsuario().equals(idUsuario)))
                    .findFirst();
            if (eq.isEmpty()) {
                return ResponseEntity.badRequest().body("La tarea es por equipo y no perteneces a ningun equipo");
            }
            equipo = eq.get();
        } else {
            alumno = iUsuario.findById(idUsuario).get();
        }
        Prorroga p = new Prorroga(tarea, alumno, equipo, req.nuevaFechaLimite());
        p.setAprobada(false);
        return ResponseEntity.status(HttpStatus.CREATED).body(iProrroga.save(p));
    }
}
