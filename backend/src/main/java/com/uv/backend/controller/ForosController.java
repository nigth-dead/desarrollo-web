package com.uv.backend.controller;

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
    public Foro editarForo(@PathVariable Integer id,
        @RequestParam Integer idUsuario,
        @RequestBody Foro foroNuevo){
        if(!iForo.existsById(id) || !iUsuario.existsById(idUsuario)){
            return null;
        }
        Foro foroBase = iForo.findById(id).get();
        if(!tienePermiso(foroBase, idUsuario)){
            return null;
        }
        if(foroNuevo.getTitulo() != null){
            foroBase.setTitulo(foroNuevo.getTitulo());
        }
        if(foroNuevo.getDescripcion() != null){
            foroBase.setDescripcion(foroNuevo.getDescripcion());
        }
        iForo.save(foroBase);
        return foroBase;
    }

    //eliminar foro
    @DeleteMapping ("/foros/{id}")
    public String eliminarForo(@PathVariable Integer id,
        @RequestParam Integer idUsuario) {
        if(!iForo.existsById(id)){
            return "El foro no existe";
        }
        if(!iUsuario.existsById(idUsuario)){
            return "El usuario no existe";
        }

        Foro foroBase = iForo.findById(id).get();

        if(!tienePermiso(foroBase, idUsuario)){
            return "Usuario no autorizado";
        }

        iForo.deleteById(id);
        return "Foro eliminado";
    }
}
