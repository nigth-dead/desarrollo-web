package com.uv.backend.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import com.uv.backend.entity.Examen;
import com.uv.backend.repository.IExamen;
import com.uv.backend.service.AccesoCurso;

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

    // ===== GET y POST =====

    @Autowired
    IExamen iExamen;
    @Autowired
    AccesoCurso accesoCurso;

    private static final Set<String> TIPOS = Set.of("OPCION_MULTIPLE", "ABIERTA");

    public record PreguntaRequest(String pregunta, String tipo, List<String> opciones,
            String respuestaCorrecta, BigDecimal valorPuntos) {}

    //lo que ve el alumno: sin la respuesta correcta
    public record PreguntaAlumno(Integer idPregunta, String pregunta, String tipo,
            String opcionesJson, BigDecimal valorPuntos) {}

    //convierte la lista de opciones a texto JSON: ["a","b"]
    private static String aJson(List<String> opciones) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < opciones.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(opciones.get(i).replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
        }
        return sb.append(']').toString();
    }

    //ver preguntas: el maestro siempre; el alumno solo durante el examen y sin respuestas
    //filtro: ?tipo=OPCION_MULTIPLE|ABIERTA
    @GetMapping ("/examenes/{idExamen}/preguntas")
    public ResponseEntity<?> listarPreguntas(@PathVariable Integer idExamen, @RequestParam Integer idUsuario,
            @RequestParam(required = false) String tipo) {
        if (tipo != null && !TIPOS.contains(tipo.toUpperCase())) {
            return ResponseEntity.badRequest().body("tipo debe ser OPCION_MULTIPLE o ABIERTA");
        }
        Optional<Examen> examen = iExamen.findById(idExamen);
        if (examen.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El examen no existe");
        }
        List<PreguntaExamen> preguntas = iPreguntaExamen.findByExamenIdExamen(idExamen).stream()
                .filter(p -> tipo == null || p.getTipo().equalsIgnoreCase(tipo))
                .toList();
        if (accesoCurso.esMaestro(examen.get().getCurso(), idUsuario)) {
            return ResponseEntity.ok(preguntas);
        }
        if (!accesoCurso.esAlumnoInscrito(examen.get().getCurso(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No perteneces a este curso");
        }
        LocalDateTime ahora = LocalDateTime.now();
        if (ahora.isBefore(examen.get().getFechaInicio()) || ahora.isAfter(examen.get().getFechaFin())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("El examen no esta disponible en este momento");
        }
        return ResponseEntity.ok(preguntas.stream()
                .map(p -> new PreguntaAlumno(p.getIdPregunta(), p.getPregunta(), p.getTipo(),
                        p.getOpcionesJson(), p.getValorPuntos()))
                .toList());
    }

    //agregar pregunta
    @PostMapping ("/examenes/{idExamen}/preguntas")
    public ResponseEntity<?> crearPregunta(@PathVariable Integer idExamen, @RequestParam Integer idUsuario,
            @RequestBody PreguntaRequest req) {
        Optional<Examen> examen = iExamen.findById(idExamen);
        if (examen.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El examen no existe");
        }
        if (!accesoCurso.esMaestro(examen.get().getCurso(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Solo el maestro del curso puede agregar preguntas");
        }
        if (req.pregunta() == null || req.pregunta().isBlank() || req.tipo() == null || req.valorPuntos() == null) {
            return ResponseEntity.badRequest().body("pregunta, tipo y valorPuntos son obligatorios");
        }
        String tipo = req.tipo().toUpperCase();
        if (!TIPOS.contains(tipo)) {
            return ResponseEntity.badRequest().body("tipo debe ser OPCION_MULTIPLE o ABIERTA");
        }
        if (req.valorPuntos().signum() <= 0) {
            return ResponseEntity.badRequest().body("valorPuntos debe ser mayor a 0");
        }
        String opcionesJson = null;
        String respuestaCorrecta = null;
        if (tipo.equals("OPCION_MULTIPLE")) {
            if (req.opciones() == null || req.opciones().size() < 2) {
                return ResponseEntity.badRequest().body("Una pregunta de opcion multiple necesita al menos 2 opciones");
            }
            if (req.respuestaCorrecta() == null || !req.opciones().contains(req.respuestaCorrecta())) {
                return ResponseEntity.badRequest().body("respuestaCorrecta debe ser una de las opciones");
            }
            opcionesJson = aJson(req.opciones());
            respuestaCorrecta = req.respuestaCorrecta();
        }
        PreguntaExamen p = new PreguntaExamen(examen.get(), req.pregunta(), tipo, opcionesJson,
                respuestaCorrecta, req.valorPuntos());
        return ResponseEntity.status(HttpStatus.CREATED).body(iPreguntaExamen.save(p));
    }
}
