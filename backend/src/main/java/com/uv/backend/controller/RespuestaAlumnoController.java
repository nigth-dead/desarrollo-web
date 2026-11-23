package com.uv.backend.controller;

import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

import com.uv.backend.entity.ExamenResultado;
import com.uv.backend.entity.RespuestaAlumno;
import com.uv.backend.repository.IExamenResultado;
import com.uv.backend.repository.IRespuestaAlumno;
import com.uv.backend.repository.IUsuario;

@RestController 
@RequestMapping ("/api")
public class RespuestaAlumnoController {
    @Autowired
    IRespuestaAlumno iRespuestaAlumno;
    @Autowired
    IUsuario iUsuario;

    //metodo para validar permisos de modificacion
    private boolean tienePermiso(RespuestaAlumno respuesta, Integer idUsuario) {
        return respuesta.getResultado() != null &&
                respuesta.getResultado().getExamen() != null &&
                respuesta.getResultado().getExamen().getCurso() != null &&
                respuesta.getResultado().getExamen().getCurso().getMaestro() != null &&
                respuesta.getResultado().getExamen().getCurso().getMaestro().getIdUsuario().equals(idUsuario);
    }

    //calificar respuesta abierta
    @PatchMapping ("/respuestas/{id}")
    public ResponseEntity<?> calificarRespuesta(@PathVariable Integer id,
        @RequestParam Integer idUsuario,
        @RequestBody RespuestaAlumno respuestaNueva) {
        if (!iRespuestaAlumno.existsById(id) || !iUsuario.existsById(idUsuario)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("La respuesta no existe");
        }
        RespuestaAlumno respuestaBase = iRespuestaAlumno.findById(id).get();
        if (!tienePermiso(respuestaBase, idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Usuario no autorizado");
        }
        if (respuestaNueva.getPuntajeObtenido() != null) {
            //el puntaje debe estar entre 0 y el valor de la pregunta
            BigDecimal puntaje = respuestaNueva.getPuntajeObtenido();
            BigDecimal maximo = respuestaBase.getPregunta() != null ? respuestaBase.getPregunta().getValorPuntos() : null;
            if (puntaje.signum() < 0 || (maximo != null && puntaje.compareTo(maximo) > 0)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El puntaje debe estar entre 0 y el valor de la pregunta");
            }
            respuestaBase.setPuntajeObtenido(puntaje);
        }
        if (respuestaNueva.getComentarioMaestro() != null) {
            respuestaBase.setComentarioMaestro(respuestaNueva.getComentarioMaestro());
        }
        iRespuestaAlumno.save(respuestaBase);
        recalcularResultado(respuestaBase.getResultado());
        return ResponseEntity.ok(respuestaBase);
    }

    @Autowired
    IExamenResultado iExamenResultado;

    //cuando todas las respuestas del examen ya tienen puntaje, el resultado pasa a CALIFICADO
    //y la calificacion final es la suma de los puntajes
    private void recalcularResultado(ExamenResultado resultado) {
        List<RespuestaAlumno> respuestas = iRespuestaAlumno.findByResultadoIdResultado(resultado.getIdResultado());
        boolean todasCalificadas = respuestas.stream().allMatch(r -> r.getPuntajeObtenido() != null);
        if (!todasCalificadas) {
            return;
        }
        BigDecimal total = respuestas.stream()
                .map(RespuestaAlumno::getPuntajeObtenido)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        resultado.setCalificacionFinal(total);
        resultado.setEstadoRevision("CALIFICADO");
        iExamenResultado.save(resultado);
    }
}
