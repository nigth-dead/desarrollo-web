package com.uv.backend.controller;

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
    public Equipo editarEquipo(@PathVariable Integer id,
        @RequestParam Integer idUsuario,
        @RequestBody Equipo equipoNuevo) {
        if (!iEquipo.existsById(id) || !iUsuario.existsById(idUsuario)) {
            return null;
        }

        Equipo equipoBase = iEquipo.findById(id).get();

        if (!tienePermiso(equipoBase, idUsuario)) {
            return null;
        }

        if (equipoNuevo.getNombreEquipo() != null) {
            equipoBase.setNombreEquipo(equipoNuevo.getNombreEquipo());
        }
        if (equipoNuevo.getIntegrantes() != null) {
            equipoBase.setIntegrantes(equipoNuevo.getIntegrantes());
        }

        iEquipo.save(equipoBase);
        return equipoBase;
    }

    // Eliminar equipo
    @DeleteMapping("/equipos/{id}")
    public String eliminarEquipo(@PathVariable Integer id,
        @RequestParam Integer idUsuario) {
        if (!iEquipo.existsById(id)) {
            return "El equipo no existe";
        }
        if (!iUsuario.existsById(idUsuario)) {
            return "El usuario no existe";
        }

        Equipo equipoBase = iEquipo.findById(id).get();

        if (!tienePermiso(equipoBase, idUsuario)) {
            return "Usuario no autorizado";
        }

        iEquipo.deleteById(id);
        return "Equipo eliminado";
    }

    // Eliminar integrantes del equipo
    @DeleteMapping("/equipos/{id}/integrantes/{idAlumno}")
    public String eliminarIntegrante(@PathVariable Integer id, 
        @PathVariable Integer idAlumno,
        @RequestParam Integer idUsuario) {
        if (!iEquipo.existsById(id)) {
            return "El equipo no existe";
        }
        if (!iUsuario.existsById(idUsuario)) {
            return "El usuario no existe";
        }

        Equipo equipo = iEquipo.findById(id).get();

        if (!tienePermiso(equipo, idUsuario)) {
            return "Usuario no autorizado";
        }

        boolean eliminado = equipo.getIntegrantes().removeIf(alumno -> alumno.getIdUsuario().equals(idAlumno));
        if (!eliminado) {
            return "El alumno no pertenece a este equipo";
        }

        iEquipo.save(equipo);
        return "Integrante eliminado";
    }
}