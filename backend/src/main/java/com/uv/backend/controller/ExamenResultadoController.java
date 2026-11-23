package com.uv.backend.controller;

import com.uv.backend.entity.Examen;
import com.uv.backend.entity.ExamenResultado;
import com.uv.backend.entity.PreguntaExamen;
import com.uv.backend.entity.RespuestaAlumno;
import com.uv.backend.entity.Usuario;
import com.uv.backend.repository.IExamen;
import com.uv.backend.repository.IExamenResultado;
import com.uv.backend.repository.IPreguntaExamen;
import com.uv.backend.repository.IRespuestaAlumno;
import com.uv.backend.repository.IUsuario;
import com.uv.backend.service.AccesoCurso;
import com.uv.backend.service.Filtros;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ExamenResultadoController {

    private final IExamen iExamen;
    private final IExamenResultado iExamenResultado;
    private final IPreguntaExamen iPreguntaExamen;
    private final IRespuestaAlumno iRespuestaAlumno;
    private final IUsuario iUsuario;
    private final AccesoCurso accesoCurso;

    public ExamenResultadoController(IExamen iExamen, IExamenResultado iExamenResultado,
            IPreguntaExamen iPreguntaExamen, IRespuestaAlumno iRespuestaAlumno,
            IUsuario iUsuario, AccesoCurso accesoCurso) {
        this.iExamen = iExamen;
        this.iExamenResultado = iExamenResultado;
        this.iPreguntaExamen = iPreguntaExamen;
        this.iRespuestaAlumno = iRespuestaAlumno;
        this.iUsuario = iUsuario;
        this.accesoCurso = accesoCurso;
    }

    public record RespuestaRequest(Integer idPregunta, String respuestaDada) {}

    public record ExamenRequest(List<RespuestaRequest> respuestas) {}

    // Resultado con sus respuestas
    public record ResultadoDetalle(ExamenResultado resultado, List<?> respuestas) {}

    // Lo que ve el alumno mientras el examen sigue abierto: sin la respuesta correcta
    public record PreguntaVista(Integer idPregunta, String pregunta, String tipo,
            String opcionesJson, BigDecimal valorPuntos) {}

    public record RespuestaVista(Integer idRespuesta, Integer idResultado, String respuestaDada,
            BigDecimal puntajeObtenido, String comentarioMaestro, PreguntaVista pregunta) {}

    // El maestro siempre ve todo. El alumno ve las respuestas correctas solo cuando el examen ya cerro;
    // antes, quien termina primero podria pasarselas a sus companeros.
    private List<?> respuestasPara(ExamenResultado r, List<RespuestaAlumno> respuestas, Integer idUsuario) {
        Examen examen = r.getExamen();
        boolean esMaestro = accesoCurso.esMaestro(examen.getCurso(), idUsuario);
        boolean cerrado = LocalDateTime.now().isAfter(examen.getFechaFin());
        if (esMaestro || cerrado) {
            return respuestas;
        }
        return respuestas.stream().map(ra -> {
            PreguntaExamen p = ra.getPregunta();
            PreguntaVista pv = p == null ? null : new PreguntaVista(p.getIdPregunta(), p.getPregunta(),
                    p.getTipo(), p.getOpcionesJson(), p.getValorPuntos());
            return new RespuestaVista(ra.getIdRespuesta(), r.getIdResultado(), ra.getRespuestaDada(),
                    ra.getPuntajeObtenido(), ra.getComentarioMaestro(), pv);
        }).toList();
    }

    // Enviar examen resuelto (POST /examenes/{id}/resultados?idUsuario=)
    // Las de opcion multiple se califican solas; las abiertas quedan pendientes
    @PostMapping("/examenes/{idExamen}/resultados")
    public ResponseEntity<?> enviar(@PathVariable Integer idExamen, @RequestParam Integer idUsuario,
            @RequestBody ExamenRequest req) {
        Optional<Examen> opt = iExamen.findById(idExamen);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El examen no existe");
        }
        Examen examen = opt.get();
        if (!accesoCurso.esAlumnoInscrito(examen.getCurso(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No estas inscrito en este curso");
        }
        LocalDateTime ahora = LocalDateTime.now();
        if (ahora.isBefore(examen.getFechaInicio()) || ahora.isAfter(examen.getFechaFin())) {
            return ResponseEntity.badRequest().body("El examen no esta disponible en este momento");
        }
        if (iExamenResultado.findByExamenIdExamenAndAlumnoIdUsuario(idExamen, idUsuario).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya enviaste este examen");
        }
        if (req.respuestas() == null) {
            return ResponseEntity.badRequest().body("respuestas es obligatorio");
        }

        Map<Integer, PreguntaExamen> preguntas = new HashMap<>();
        for (PreguntaExamen p : iPreguntaExamen.findByExamenIdExamen(idExamen)) {
            preguntas.put(p.getIdPregunta(), p);
        }
        Map<Integer, String> dadas = new HashMap<>();
        for (RespuestaRequest r : req.respuestas()) {
            if (r.idPregunta() == null || !preguntas.containsKey(r.idPregunta())) {
                return ResponseEntity.badRequest().body("La pregunta " + r.idPregunta() + " no es de este examen");
            }
            dadas.put(r.idPregunta(), r.respuestaDada());
        }

        Usuario alumno = iUsuario.findById(idUsuario).get();
        ExamenResultado resultado = iExamenResultado.save(new ExamenResultado(examen, alumno, "PENDIENTE_MANUAL"));

        boolean hayAbiertas = false;
        BigDecimal total = BigDecimal.ZERO;
        List<RespuestaAlumno> guardadas = new ArrayList<>();
        // Se registra una respuesta por cada pregunta (las no contestadas quedan vacias)
        for (PreguntaExamen p : preguntas.values()) {
            String dada = dadas.get(p.getIdPregunta());
            RespuestaAlumno ra = new RespuestaAlumno(resultado, p, dada);
            if ("ABIERTA".equals(p.getTipo())) {
                hayAbiertas = true;
            } else {
                boolean correcta = dada != null && p.getRespuestaCorrecta() != null
                        && dada.trim().equalsIgnoreCase(p.getRespuestaCorrecta().trim());
                BigDecimal puntos = correcta ? p.getValorPuntos() : BigDecimal.ZERO;
                ra.setPuntajeObtenido(puntos);
                total = total.add(puntos);
            }
            guardadas.add(iRespuestaAlumno.save(ra));
        }

        if (!hayAbiertas) {
            resultado.setEstadoRevision("CALIFICADO");
            resultado.setCalificacionFinal(total);
            resultado = iExamenResultado.save(resultado);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResultadoDetalle(resultado, respuestasPara(resultado, guardadas, idUsuario)));
    }

    // Ver resultados de todos (GET /examenes/{id}/resultados?idUsuario=) - maestro
    // Filtros: ?estado=PENDIENTE_MANUAL|CALIFICADO, ?minCalificacion=, ?maxCalificacion=
    @GetMapping("/examenes/{idExamen}/resultados")
    public ResponseEntity<?> listar(@PathVariable Integer idExamen, @RequestParam Integer idUsuario,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) BigDecimal minCalificacion,
            @RequestParam(required = false) BigDecimal maxCalificacion) {
        if (!Filtros.valorValido(estado, "PENDIENTE_MANUAL", "CALIFICADO")) {
            return ResponseEntity.badRequest().body("estado debe ser PENDIENTE_MANUAL o CALIFICADO");
        }
        Optional<Examen> examen = iExamen.findById(idExamen);
        if (examen.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El examen no existe");
        }
        if (!accesoCurso.esMaestro(examen.get().getCurso(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Solo el maestro del curso puede ver todos los resultados");
        }
        return ResponseEntity.ok(iExamenResultado.findByExamenIdExamen(idExamen).stream()
                .filter(r -> estado == null || r.getEstadoRevision().equalsIgnoreCase(estado))
                // con filtro de calificacion solo entran los que ya tienen calificacion
                .filter(r -> minCalificacion == null
                        || (r.getCalificacionFinal() != null && r.getCalificacionFinal().compareTo(minCalificacion) >= 0))
                .filter(r -> maxCalificacion == null
                        || (r.getCalificacionFinal() != null && r.getCalificacionFinal().compareTo(maxCalificacion) <= 0))
                .toList());
    }

    // Ver mi resultado (GET /examenes/{id}/resultados/mio?idUsuario=)
    @GetMapping("/examenes/{idExamen}/resultados/mio")
    public ResponseEntity<?> mio(@PathVariable Integer idExamen, @RequestParam Integer idUsuario) {
        if (!iExamen.existsById(idExamen)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El examen no existe");
        }
        return iExamenResultado.findByExamenIdExamenAndAlumnoIdUsuario(idExamen, idUsuario)
                .<ResponseEntity<?>>map(r -> ResponseEntity.ok(
                        new ResultadoDetalle(r, respuestasPara(r,
                                iRespuestaAlumno.findByResultadoIdResultado(r.getIdResultado()), idUsuario))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aun no has presentado este examen"));
    }

    // Ver resultado con respuestas (GET /resultados/{id}?idUsuario=) - maestro o el alumno
    @GetMapping("/resultados/{id}")
    public ResponseEntity<?> obtener(@PathVariable Integer id, @RequestParam Integer idUsuario) {
        Optional<ExamenResultado> opt = iExamenResultado.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El resultado no existe");
        }
        ExamenResultado r = opt.get();
        boolean esAlumno = r.getAlumno().getIdUsuario().equals(idUsuario);
        if (!esAlumno && !accesoCurso.esMaestro(r.getExamen().getCurso(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Usuario no autorizado");
        }
        return ResponseEntity.ok(new ResultadoDetalle(r,
                respuestasPara(r, iRespuestaAlumno.findByResultadoIdResultado(id), idUsuario)));
    }
}
