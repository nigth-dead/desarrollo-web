package com.uv.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uv.backend.entity.PreguntaExamen;
import com.uv.backend.repository.IPreguntaExamen;
import com.uv.backend.repository.IUsuario;

@RestController 
@RequestMapping ("/api")
public class PreguntaExamenController {
    @Autowired
    IPreguntaExamen iPreguntaExamen;
    @Autowired
    IUsuario iUsuario;

    //metodo para validar permisos de modificacion
    private boolean tienePermiso(PreguntaExamen pregunta, Integer idUsuario) {
        return pregunta.getExamen() != null &&
                pregunta.getExamen().getCurso() != null &&
                pregunta.getExamen().getCurso().getMaestro() != null &&
                pregunta.getExamen().getCurso().getMaestro().getIdUsuario().equals(idUsuario);
    }

    //editar pregunta
    @PutMapping ("/preguntas/{id}")
    public PreguntaExamen editarPregunta(@PathVariable Integer id,
        @RequestParam Integer idUsuario,
        @RequestBody PreguntaExamen preguntaNueva) {
        if (!iPreguntaExamen.existsById(id) || !iUsuario.existsById(idUsuario)) {
            return null;
        }
        PreguntaExamen preguntaBase = iPreguntaExamen.findById(id).get();
        if (!tienePermiso(preguntaBase, idUsuario)) {
            return null;
        }
        if (preguntaNueva.getPregunta() != null) {
            preguntaBase.setPregunta(preguntaNueva.getPregunta());
        }
        if (preguntaNueva.getTipo() != null) {
            preguntaBase.setTipo(preguntaNueva.getTipo());
        }
        if (preguntaNueva.getOpcionesJson() != null) {
            preguntaBase.setOpcionesJson(preguntaNueva.getOpcionesJson());
        }
        if (preguntaNueva.getRespuestaCorrecta() != null) {
            preguntaBase.setRespuestaCorrecta(preguntaNueva.getRespuestaCorrecta());
        }
        if (preguntaNueva.getValorPuntos() != null) {
            preguntaBase.setValorPuntos(preguntaNueva.getValorPuntos());
        }
        iPreguntaExamen.save(preguntaBase);
        return preguntaBase;
    }

    //eliminar pregunta
    @DeleteMapping ("/preguntas/{id}")
    public String eliminarPregunta(@PathVariable Integer id,
        @RequestParam Integer idUsuario) {
        if (!iPreguntaExamen.existsById(id)) {
            return "La pregunta no existe";
        }
        if (!iUsuario.existsById(idUsuario)) {
            return "El usuario no existe";
        }
        PreguntaExamen preguntaBase = iPreguntaExamen.findById(id).get();
        if (!tienePermiso(preguntaBase, idUsuario)) {
            return "Usuario no autorizado";
        }
        iPreguntaExamen.deleteById(id);
        return "Pregunta eliminada";
    }
}
