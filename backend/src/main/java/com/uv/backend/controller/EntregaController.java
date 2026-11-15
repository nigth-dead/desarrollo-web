package com.uv.backend.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uv.backend.entity.Entrega;
import com.uv.backend.repository.IEntrega;
import com.uv.backend.repository.IUsuario;

@RestController 
@RequestMapping ("/api")
public class EntregaController {
    @Autowired 
    IEntrega iEntrega;
    @Autowired 
    IUsuario iUsuario;

    //metodo para validar permisos de modificacion
    private boolean esDuenoOIntegrante(Entrega entrega, Integer idUsuario) {
        boolean esDuenoIndividual = entrega.getAlumno() != null &&
                entrega.getAlumno().getIdUsuario().equals(idUsuario);

        boolean esIntegranteEquipo = entrega.getEquipo() != null &&
                entrega.getEquipo().getIntegrantes() != null &&
                entrega.getEquipo().getIntegrantes().stream()
                        .anyMatch(alumno -> alumno.getIdUsuario().equals(idUsuario));

        return esDuenoIndividual || esIntegranteEquipo;
    }

    //Solo el maestro del curso puede asignar calificación y comentarios
    private boolean esMaestro(Entrega entrega, Integer idUsuario) {
        return entrega.getTarea() != null &&
                entrega.getTarea().getCurso() != null &&
                entrega.getTarea().getCurso().getMaestro() != null &&
                entrega.getTarea().getCurso().getMaestro().getIdUsuario().equals(idUsuario);
    }

    @PutMapping ("/entregas/{id}")
    public Entrega reemplazarEntrega(@PathVariable Integer id, 
        @RequestParam Integer idUsuario,
        @RequestBody Entrega entregaNueva){
        if (!iEntrega.existsById(id) || !iUsuario.existsById(idUsuario)){
            return null;
        }
        Entrega entregaBase = iEntrega.findById(id).get();

        if (esDuenoOIntegrante(entregaBase, idUsuario)) {
            if (entregaNueva.getArchivoUrl() != null) {
                entregaBase.setArchivoUrl(entregaNueva.getArchivoUrl());
                entregaBase.setFechaEntrega(LocalDateTime.now());
                iEntrega.save(entregaBase);
                return entregaBase;
            }
        }
        
        if (esMaestro(entregaBase, idUsuario)) {
            if (entregaNueva.getCalificacion() != null) {
                entregaBase.setCalificacion(entregaNueva.getCalificacion());
            }
            if (entregaNueva.getComentarioMaestro() != null) {
                entregaBase.setComentarioMaestro(entregaNueva.getComentarioMaestro());
            }
            if(entregaNueva.getCalificacion() != null ||
                entregaNueva.getComentarioMaestro() != null){
                iEntrega.save(entregaBase);
                return entregaBase;
            }
        }
        return null;
    }
}
