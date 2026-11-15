package com.uv.backend.controller;

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
}
