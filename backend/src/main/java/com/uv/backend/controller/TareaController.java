package com.uv.backend.controller;

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
}
