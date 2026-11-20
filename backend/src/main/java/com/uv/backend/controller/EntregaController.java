package com.uv.backend.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.bind.annotation.PatchMapping;
import com.uv.backend.entity.Notificacion;
import com.uv.backend.repository.INotificacion;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import com.uv.backend.entity.Equipo;
import com.uv.backend.entity.Tarea;
import com.uv.backend.entity.Usuario;
import com.uv.backend.repository.IEquipo;
import com.uv.backend.repository.IProrroga;
import com.uv.backend.repository.ITarea;
import com.uv.backend.service.AccesoCurso;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

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

    // Carpeta donde se guardan los archivos subidos
    private static final String CARPETA_SUBIDAS = "uploads";

    // ¿Es un enlace valido (http:// o https:// con dominio)?
    private boolean esEnlaceValido(String url) {
        if (url == null) return false;
        String u = url.trim();
        if (!u.startsWith("http://") && !u.startsWith("https://")) return false;
        try {
            URI uri = URI.create(u);
            return uri.getHost() != null && !uri.getHost().isBlank();
        } catch (Exception e) {
            return false;
        }
    }

    // Guarda el archivo subido en la carpeta de subidas y devuelve la URL
    private String guardarArchivo(MultipartFile archivo, HttpServletRequest request) throws IOException {
        String original = archivo.getOriginalFilename();
        if (original == null || original.isBlank()) original = "archivo";
        String nombre = original.replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._-]", "_");
        if (nombre.isBlank()) nombre = "archivo";
        String guardado = UUID.randomUUID().toString().substring(0, 8) + "_" + nombre;
        Path carpeta = Paths.get(CARPETA_SUBIDAS);
        Files.createDirectories(carpeta);
        Files.copy(archivo.getInputStream(), carpeta.resolve(guardado));
        String host = request.getHeader("Host");
        String base = host != null && !host.isBlank()
                ? request.getScheme() + "://" + host
                : request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        return base + "/uploads/" + guardado;
    }

    @PutMapping (value = "/entregas/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Entrega reemplazarEntrega(@PathVariable Integer id, 
        @RequestParam Integer idUsuario,
        @RequestBody Entrega entregaNueva){
        if (!iEntrega.existsById(id) || !iUsuario.existsById(idUsuario)){
            return null;
        }
        Entrega entregaBase = iEntrega.findById(id).get();

        if (esDuenoOIntegrante(entregaBase, idUsuario)) {
            if (entregaNueva.getArchivoUrl() != null) {
                if (!esEnlaceValido(entregaNueva.getArchivoUrl())) {
                    return null;
                }
                entregaBase.setArchivoUrl(entregaNueva.getArchivoUrl());
                entregaBase.setFechaEntrega(LocalDateTime.now());
            }
            if (entregaNueva.getDescripcion() != null) {
                entregaBase.setDescripcion(entregaNueva.getDescripcion());
            }
            if (entregaNueva.getArchivoUrl() != null || entregaNueva.getDescripcion() != null) {
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

    // Reemplazar la entrega subiendo un archivo nuevo
    @PutMapping (value = "/entregas/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> reemplazarEntregaArchivo(@PathVariable Integer id,
            @RequestParam Integer idUsuario,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            @RequestParam(value = "archivoUrl", required = false) String archivoUrl,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            HttpServletRequest request) {
        if (!iEntrega.existsById(id) || !iUsuario.existsById(idUsuario)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("La entrega no existe");
        }
        Entrega entregaBase = iEntrega.findById(id).get();
        if (!esDuenoOIntegrante(entregaBase, idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Usuario no autorizado");
        }
        boolean hayArchivo = archivo != null && !archivo.isEmpty();
        if (hayArchivo) {
            try {
                entregaBase.setArchivoUrl(guardarArchivo(archivo, request));
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("No se pudo guardar el archivo");
            }
            entregaBase.setFechaEntrega(LocalDateTime.now());
        } else if (archivoUrl != null && !archivoUrl.isBlank()) {
            if (!esEnlaceValido(archivoUrl)) {
                return ResponseEntity.badRequest().body("archivoUrl debe ser un enlace válido (http:// o https://)");
            }
            entregaBase.setArchivoUrl(archivoUrl.trim());
            entregaBase.setFechaEntrega(LocalDateTime.now());
        }
        if (descripcion != null) {
            entregaBase.setDescripcion(descripcion);
        }
        return ResponseEntity.ok(iEntrega.save(entregaBase));
    }

    // Eliminar entrega (DELETE /entregas/{id}?idUsuario=)
    // Solo quien la hizo o su equipo, y solo si la tarea sigue abierta.
    @DeleteMapping("/entregas/{id}")
    public ResponseEntity<?> eliminarEntrega(@PathVariable Integer id, @RequestParam Integer idUsuario) {
        Optional<Entrega> opt = iEntrega.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("La entrega no existe");
        }
        Entrega entrega = opt.get();
        if (!esDuenoOIntegrante(entrega, idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Usuario no autorizado");
        }
        Tarea tarea = entrega.getTarea();
        if (tarea != null) {
            LocalDateTime limite = fechaLimiteDe(tarea, idUsuario, entrega.getEquipo());
            if (limite != null && LocalDateTime.now().isAfter(limite)) {
                return ResponseEntity.badRequest().body("La fecha limite ya paso");
            }
        }
        iEntrega.delete(entrega);
        return ResponseEntity.ok("Entrega eliminada");
    }

    // ===== GET y POST =====

    @Autowired
    ITarea iTarea;
    @Autowired
    IEquipo iEquipo;
    @Autowired
    IProrroga iProrroga;
    @Autowired
    AccesoCurso accesoCurso;

    public record EntregaRequest(String archivoUrl, String descripcion) {}

    //busca el equipo del alumno dentro de una tarea por equipos
    private Optional<Equipo> equipoDelAlumno(Tarea tarea, Integer idAlumno) {
        return iEquipo.findByTareaIdTarea(tarea.getIdTarea()).stream()
                .filter(e -> e.getIntegrantes().stream().anyMatch(u -> u.getIdUsuario().equals(idAlumno)))
                .findFirst();
    }

    //busca la entrega del alumno (individual) o de su equipo
    private Optional<Entrega> entregaDelAlumno(Tarea tarea, Integer idAlumno) {
        Optional<Equipo> equipo = Boolean.TRUE.equals(tarea.getEsEquipo())
                ? equipoDelAlumno(tarea, idAlumno) : Optional.empty();
        return iEntrega.findByTareaIdTarea(tarea.getIdTarea()).stream()
                .filter(e -> (e.getAlumno() != null && e.getAlumno().getIdUsuario().equals(idAlumno))
                        || (equipo.isPresent() && e.getEquipo() != null
                                && e.getEquipo().getIdEquipo().equals(equipo.get().getIdEquipo())))
                .findFirst();
    }

    //fecha limite real: la de la tarea o la de una prorroga aprobada
    private LocalDateTime fechaLimiteDe(Tarea tarea, Integer idAlumno, Equipo equipo) {
        LocalDateTime limite = tarea.getFechaLimite();
        for (var p : iProrroga.findByTareaIdTarea(tarea.getIdTarea())) {
            boolean esSuya = (p.getAlumno() != null && p.getAlumno().getIdUsuario().equals(idAlumno))
                    || (equipo != null && p.getEquipo() != null && p.getEquipo().getIdEquipo().equals(equipo.getIdEquipo()));
            if (esSuya && Boolean.TRUE.equals(p.getAprobada())
                    && (limite == null || p.getNuevaFechaLimite().isAfter(limite))) {
                limite = p.getNuevaFechaLimite();
            }
        }
        return limite;
    }

    //ver entregas de una tarea (maestro)
    //filtros: ?calificada=true|false, ?tarde=true|false (entregada despues de la fecha limite de la tarea)
    @GetMapping ("/tareas/{idTarea}/entregas")
    public ResponseEntity<?> listarEntregas(@PathVariable Integer idTarea, @RequestParam Integer idUsuario,
            @RequestParam(required = false) Boolean calificada,
            @RequestParam(required = false) Boolean tarde) {
        Optional<Tarea> tarea = iTarea.findById(idTarea);
        if (tarea.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("La tarea no existe");
        }
        if (!accesoCurso.esMaestro(tarea.get().getCurso(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Solo el maestro del curso puede ver todas las entregas");
        }
        LocalDateTime limite = tarea.get().getFechaLimite();
        return ResponseEntity.ok(iEntrega.findByTareaIdTarea(idTarea).stream()
                .filter(e -> calificada == null || calificada == (e.getCalificacion() != null))
                .filter(e -> {
                    if (tarde == null) {
                        return true;
                    }
                    boolean fueTarde = limite != null && e.getFechaEntrega() != null
                            && e.getFechaEntrega().isAfter(limite);
                    return tarde == fueTarde;
                })
                .toList());
    }

    //ver mi entrega (o la de mi equipo)
    @GetMapping ("/tareas/{idTarea}/entregas/mia")
    public ResponseEntity<?> miEntrega(@PathVariable Integer idTarea, @RequestParam Integer idUsuario) {
        Optional<Tarea> tarea = iTarea.findById(idTarea);
        if (tarea.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("La tarea no existe");
        }
        if (!accesoCurso.esAlumnoInscrito(tarea.get().getCurso(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No estas inscrito en este curso");
        }
        return entregaDelAlumno(tarea.get(), idUsuario)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aun no has entregado esta tarea"));
    }

    //entregar tarea con un enlace (JSON). Si se sube un archivo se usa
    //la variante multipart de abajo.
    @PostMapping (value = "/tareas/{idTarea}/entregas", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> entregar(@PathVariable Integer idTarea, @RequestParam Integer idUsuario,
            @RequestBody EntregaRequest req) {
        Optional<Tarea> opt = iTarea.findById(idTarea);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("La tarea no existe");
        }
        Tarea tarea = opt.get();
        if (!accesoCurso.esAlumnoInscrito(tarea.getCurso(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No estas inscrito en este curso");
        }
        if (req.archivoUrl() == null || req.archivoUrl().isBlank()) {
            return ResponseEntity.badRequest().body("archivoUrl es obligatorio");
        }
        if (!esEnlaceValido(req.archivoUrl())) {
            return ResponseEntity.badRequest().body("archivoUrl debe ser un enlace válido (http:// o https://)");
        }
        Usuario alumno = iUsuario.findById(idUsuario).get();
        Equipo equipo = null;
        if (Boolean.TRUE.equals(tarea.getEsEquipo())) {
            Optional<Equipo> eq = equipoDelAlumno(tarea, idUsuario);
            if (eq.isEmpty()) {
                return ResponseEntity.badRequest().body("La tarea es por equipo y no perteneces a ningun equipo");
            }
            equipo = eq.get();
        }
        if (entregaDelAlumno(tarea, idUsuario).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya existe una entrega; usa PUT para reemplazarla");
        }
        LocalDateTime limite = fechaLimiteDe(tarea, idUsuario, equipo);
        if (limite != null && LocalDateTime.now().isAfter(limite)) {
            return ResponseEntity.badRequest().body("La fecha limite ya paso");
        }
        //individual: se llena id_alumno; por equipo: se llena id_equipo
        Entrega entrega = new Entrega(tarea, equipo == null ? alumno : null, equipo, req.archivoUrl());
        entrega.setDescripcion(req.descripcion());
        entrega.setFechaEntrega(LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(iEntrega.save(entrega));
    }

    //entregar tarea subiendo un archivo
    @PostMapping (value = "/tareas/{idTarea}/entregas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> entregarArchivo(@PathVariable Integer idTarea, @RequestParam Integer idUsuario,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            @RequestParam(value = "archivoUrl", required = false) String archivoUrl,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            HttpServletRequest request) {
        Optional<Tarea> opt = iTarea.findById(idTarea);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("La tarea no existe");
        }
        Tarea tarea = opt.get();
        if (!accesoCurso.esAlumnoInscrito(tarea.getCurso(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No estas inscrito en este curso");
        }
        boolean hayArchivo = archivo != null && !archivo.isEmpty();
        if (!hayArchivo && (archivoUrl == null || archivoUrl.isBlank())) {
            return ResponseEntity.badRequest().body("Sube un archivo o pega un enlace válido");
        }
        if (!hayArchivo && !esEnlaceValido(archivoUrl)) {
            return ResponseEntity.badRequest().body("archivoUrl debe ser un enlace válido (http:// o https://)");
        }
        Usuario alumno = iUsuario.findById(idUsuario).get();
        Equipo equipo = null;
        if (Boolean.TRUE.equals(tarea.getEsEquipo())) {
            Optional<Equipo> eq = equipoDelAlumno(tarea, idUsuario);
            if (eq.isEmpty()) {
                return ResponseEntity.badRequest().body("La tarea es por equipo y no perteneces a ningun equipo");
            }
            equipo = eq.get();
        }
        if (entregaDelAlumno(tarea, idUsuario).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya existe una entrega; usa PUT para reemplazarla");
        }
        LocalDateTime limite = fechaLimiteDe(tarea, idUsuario, equipo);
        if (limite != null && LocalDateTime.now().isAfter(limite)) {
            return ResponseEntity.badRequest().body("La fecha limite ya paso");
        }
        String urlFinal;
        if (hayArchivo) {
            try {
                urlFinal = guardarArchivo(archivo, request);
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("No se pudo guardar el archivo");
            }
        } else {
            urlFinal = archivoUrl.trim();
        }
        //individual: se llena id_alumno; por equipo: se llena id_equipo
        Entrega entrega = new Entrega(tarea, equipo == null ? alumno : null, equipo, urlFinal);
        entrega.setDescripcion(descripcion);
        entrega.setFechaEntrega(LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(iEntrega.save(entrega));
    }

    //ver entrega (maestro o autor)
    @GetMapping ("/entregas/{id}")
    public ResponseEntity<?> obtenerEntrega(@PathVariable Integer id, @RequestParam Integer idUsuario) {
        Optional<Entrega> entrega = iEntrega.findById(id);
        if (entrega.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("La entrega no existe");
        }
        if (!esMaestro(entrega.get(), idUsuario) && !esDuenoOIntegrante(entrega.get(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Usuario no autorizado");
        }
        return ResponseEntity.ok(entrega.get());
    }

    //descargar archivo: redirige a la URL del archivo
    @GetMapping ("/entregas/{id}/archivo")
    public ResponseEntity<?> descargarArchivo(@PathVariable Integer id, @RequestParam Integer idUsuario) {
        Optional<Entrega> entrega = iEntrega.findById(id);
        if (entrega.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("La entrega no existe");
        }
        if (!esMaestro(entrega.get(), idUsuario) && !esDuenoOIntegrante(entrega.get(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Usuario no autorizado");
        }
        if (entrega.get().getArchivoUrl() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("La entrega no tiene archivo");
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(entrega.get().getArchivoUrl())).build();
    }

    // ===== PATCH =====

    @Autowired
    INotificacion iNotificacion;

    public record CalificacionRequest(BigDecimal calificacion, String comentarioMaestro) {}

    //calificar entrega (solo el maestro del curso). Avisa al alumno o a los integrantes del equipo
    @PatchMapping ("/entregas/{id}/calificacion")
    public ResponseEntity<?> calificarEntrega(@PathVariable Integer id, @RequestParam Integer idUsuario,
            @RequestBody CalificacionRequest req) {
        Optional<Entrega> opt = iEntrega.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("La entrega no existe");
        }
        Entrega entrega = opt.get();
        if (!esMaestro(entrega, idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Solo el maestro del curso puede calificar");
        }
        if (req.calificacion() == null) {
            return ResponseEntity.badRequest().body("calificacion es obligatoria");
        }
        if (req.calificacion().signum() < 0 || req.calificacion().compareTo(BigDecimal.valueOf(100)) > 0) {
            return ResponseEntity.badRequest().body("calificacion debe estar entre 0 y 100");
        }
        entrega.setCalificacion(req.calificacion());
        if (req.comentarioMaestro() != null) {
            entrega.setComentarioMaestro(req.comentarioMaestro());
        }
        Entrega guardada = iEntrega.save(entrega);

        String mensaje = "Tu entrega de \"" + entrega.getTarea().getTitulo() + "\" fue calificada: "
                + req.calificacion().stripTrailingZeros().toPlainString();
        List<Usuario> destinatarios = entrega.getAlumno() != null
                ? List.of(entrega.getAlumno())
                : entrega.getEquipo().getIntegrantes();
        for (Usuario u : destinatarios) {
            Notificacion n = new Notificacion(u, mensaje);
            n.setLeido(false);
            iNotificacion.save(n);
        }
        return ResponseEntity.ok(guardada);
    }
}
