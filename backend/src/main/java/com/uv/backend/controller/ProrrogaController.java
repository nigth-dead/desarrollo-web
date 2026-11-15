package com.uv.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
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
    @PutMapping ("/prorrogas/{id}")
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
}
