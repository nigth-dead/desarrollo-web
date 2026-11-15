package com.uv.backend.controller;

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
    public Examen editarExamen(@PathVariable Integer id,
        @RequestParam Integer idUsuario,
        @RequestBody Examen examenNuevo) {
        if (!iExamen.existsById(id) || !iUsuario.existsById(idUsuario)) {
            return null;
        }
        Examen examenBase = iExamen.findById(id).get();
        if (!tienePermiso(examenBase, idUsuario)) {
            return null;
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
        return examenBase;
    }

    //eliminar examen
    @DeleteMapping ("/examenes/{id}")
    public String eliminarExamen(@PathVariable Integer id,
        @RequestParam Integer idUsuario) {
        if (!iExamen.existsById(id)) {
            return "El examen no existe";
        }
        if (!iUsuario.existsById(idUsuario)) {
            return "El usuario no existe";
        }
        Examen examenBase = iExamen.findById(id).get();
        if (!tienePermiso(examenBase, idUsuario)) {
            return "Usuario no autorizado";
        }
        iExamen.deleteById(id);
        return "Examen eliminado";
    }
}
