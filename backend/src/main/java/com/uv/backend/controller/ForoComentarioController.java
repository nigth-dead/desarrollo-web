package com.uv.backend.controller;

import java.util.Comparator;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import com.uv.backend.entity.Foro;
import com.uv.backend.repository.IForo;
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

import com.uv.backend.entity.ForoComentario;
import com.uv.backend.repository.IForoComentario;
import com.uv.backend.repository.IUsuario;

@RestController 
@RequestMapping ("/api")
public class ForoComentarioController {
    @Autowired
    IForoComentario iForoComentario;
    @Autowired
    IUsuario iUsuario;

    //Solo el autor puede editar su comentario
    private boolean esAutor(ForoComentario comentario, Integer idUsuario) {
        return comentario.getUsuario() != null &&
                comentario.getUsuario().getIdUsuario().equals(idUsuario);
    }

    //el maestro del curso puede eliminar comentarios de otros
    private boolean esMaestroCurso(ForoComentario comentario, Integer idUsuario) {
        return comentario.getForo() != null &&
                comentario.getForo().getCurso() != null &&
                comentario.getForo().getCurso().getMaestro() != null &&
                comentario.getForo().getCurso().getMaestro().getIdUsuario().equals(idUsuario);
    }

    //editar comentario
    @PutMapping ("/comentarios/{id}")
    public ForoComentario editarComentario(@PathVariable Integer id,
        @RequestParam Integer idUsuario,
        @RequestBody ForoComentario comentarioNuevo) {
        if (!iForoComentario.existsById(id) || !iUsuario.existsById(idUsuario)) {
            return null;
        }
        ForoComentario comentarioBase = iForoComentario.findById(id).get();
        if (!esAutor(comentarioBase, idUsuario)) {
            return null;
        }
        if (comentarioNuevo.getContenido() != null) {
            comentarioBase.setContenido(comentarioNuevo.getContenido());
        }
        iForoComentario.save(comentarioBase);
        return comentarioBase;
    }

    //eliminar comentario
    @DeleteMapping ("/comentarios/{id}")
    public String eliminarComentario(@PathVariable Integer id,
        @RequestParam Integer idUsuario) {
        if (!iForoComentario.existsById(id)) {
            return "El comentario no existe";
        }
        if (!iUsuario.existsById(idUsuario)) {
            return "El usuario no existe";
        }
        ForoComentario comentarioBase = iForoComentario.findById(id).get();
        if (!esAutor(comentarioBase, idUsuario) && !esMaestroCurso(comentarioBase, idUsuario)) {
            return "Usuario no autorizado";
        }
        iForoComentario.deleteById(id);
        return "Comentario eliminado";
    }

    // ===== GET y POST =====

    @Autowired
    IForo iForo;
    @Autowired
    AccesoCurso accesoCurso;

    public record ComentarioRequest(String contenido) {}

    //ver comentarios del foro (orden cronologico)
    //filtros: ?idAutor= (comentarios de un usuario), ?q= (texto del comentario)
    @GetMapping ("/foros/{idForo}/comentarios")
    public ResponseEntity<?> listarComentarios(@PathVariable Integer idForo, @RequestParam Integer idUsuario,
            @RequestParam(required = false) Integer idAutor,
            @RequestParam(required = false) String q) {
        Optional<Foro> foro = iForo.findById(idForo);
        if (foro.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El foro no existe");
        }
        if (!accesoCurso.esMiembro(foro.get().getCurso(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No perteneces a este curso");
        }
        return ResponseEntity.ok(iForoComentario.findByForoIdForo(idForo).stream()
                .filter(c -> idAutor == null || c.getUsuario().getIdUsuario().equals(idAutor))
                .filter(c -> Filtros.contiene(q, c.getContenido()))
                .sorted(Comparator.comparing(ForoComentario::getIdComentario))
                .toList());
    }

    //comentar
    @PostMapping ("/foros/{idForo}/comentarios")
    public ResponseEntity<?> comentar(@PathVariable Integer idForo, @RequestParam Integer idUsuario,
            @RequestBody ComentarioRequest req) {
        Optional<Foro> foro = iForo.findById(idForo);
        if (foro.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El foro no existe");
        }
        if (!accesoCurso.esMiembro(foro.get().getCurso(), idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No perteneces a este curso");
        }
        if (req.contenido() == null || req.contenido().isBlank()) {
            return ResponseEntity.badRequest().body("contenido es obligatorio");
        }
        ForoComentario c = new ForoComentario(foro.get(), iUsuario.findById(idUsuario).get(), req.contenido());
        return ResponseEntity.status(HttpStatus.CREATED).body(iForoComentario.save(c));
    }
}
