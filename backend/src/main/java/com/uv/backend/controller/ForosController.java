package com.uv.backend.controller;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import com.uv.backend.entity.Curso;
import com.uv.backend.repository.ICurso;
import com.uv.backend.service.AccesoCurso;
import com.uv.backend.service.Filtros;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uv.backend.entity.Foro;
import com.uv.backend.repository.IForo;
import com.uv.backend.repository.IUsuario;

@RestController 
@RequestMapping ("/api")
public class ForosController {
    @Autowired 
    IForo iForo;
    @Autowired
    IUsuario iUsuario;

    //metodo para validar permisos de modificacion
    private boolean tienePermiso(Foro foro, Integer idUsuario) {
        return foro.getCurso() != null &&
                foro.getCurso().getMaestro() != null &&
                foro.getCurso().getMaestro().getIdUsuario().equals(idUsuario);
    }

    @PutMapping ("/foros/{id}")
    public ResponseEntity<?> editarForo(@PathVariable Integer id,
        @RequestParam Integer idUsuario,
        @RequestBody Foro foroNuevo){
        if(!iForo.existsById(id) || !iUsuario.existsById(idUsuario)){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El foro no existe");
        }
        Foro foroBase = iForo.findById(id).get();
        if(!tienePermiso(foroBase, idUsuario)){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Usuario no autorizado");
        }
        if(foroNuevo.getTitulo() != null){
            foroBase.setTitulo(foroNuevo.getTitulo());
        }
        if(foroNuevo.getDescripcion() != null){
            foroBase.setDescripcion(foroNuevo.getDescripcion());
        }
        iForo.save(foroBase);
        return ResponseEntity.ok(foroBase);
    }

    //eliminar foro
    @DeleteMapping ("/foros/{id}")
    public ResponseEntity<?> eliminarForo(@PathVariable Integer id,
        @RequestParam Integer idUsuario) {
        if(!iForo.existsById(id)){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El foro no existe");
        }
        if(!iUsuario.existsById(idUsuario)){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El usuario no existe");
        }

        Foro foroBase = iForo.findById(id).get();

        if(!tienePermiso(foroBase, idUsuario)){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Usuario no autorizado");
        }

        iForo.deleteById(id);
        return ResponseEntity.ok("Foro eliminado");
    }

    // ===== GET y POST =====

    @Autowired
    ICurso iCurso;
    @Autowired
    AccesoCurso accesoCurso;

    public record ForoRequest(String titulo, String descripcion) {}

    //ver foros del curso
    //filtro: ?q= (titulo o descripcion)
    @GetMapping ("/cursos/{idCurso}/foros")
    public ResponseEntity<?> listarForos(@PathVariable Integer idCurso, @RequestParam Integer idUsuario,
            @RequestParam(required = false) String q) {
        Optional<Curso> curso = iCurso.findById(idCurso);
        if (curso.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El curso no existe");
        }
        if (!accesoCurso.esMiembro(curso.get(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No perteneces a este curso");
        }
        return ResponseEntity.ok(iForo.findByCursoIdCurso(idCurso).stream()
                .filter(f -> Filtros.contiene(q, f.getTitulo(), f.getDescripcion()))
                .toList());
    }

    //crear foro
    @PostMapping ("/cursos/{idCurso}/foros")
    public ResponseEntity<?> crearForo(@PathVariable Integer idCurso, @RequestParam Integer idUsuario,
            @RequestBody ForoRequest req) {
        Optional<Curso> curso = iCurso.findById(idCurso);
        if (curso.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El curso no existe");
        }
        if (!accesoCurso.esMaestro(curso.get(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Solo el maestro del curso puede crear foros");
        }
        if (req.titulo() == null || req.titulo().isBlank()) {
            return ResponseEntity.badRequest().body("titulo es obligatorio");
        }
        Foro foro = iForo.save(new Foro(curso.get(), req.titulo(), req.descripcion()));
        return ResponseEntity.status(HttpStatus.CREATED).body(foro);
    }

    //ver foro
    @GetMapping ("/foros/{id}")
    public ResponseEntity<?> obtenerForo(@PathVariable Integer id, @RequestParam Integer idUsuario) {
        Optional<Foro> foro = iForo.findById(id);
        if (foro.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El foro no existe");
        }
        if (!accesoCurso.esMiembro(foro.get().getCurso(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No perteneces a este curso");
        }
        return ResponseEntity.ok(foro.get());
    }
}
