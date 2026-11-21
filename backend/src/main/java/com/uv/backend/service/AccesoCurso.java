package com.uv.backend.service;

import com.uv.backend.entity.Curso;
import com.uv.backend.entity.CursoAlumnoId;
import com.uv.backend.repository.ICursoAlumno;
import org.springframework.stereotype.Service;

/**
 * Validaciones de acceso a un curso que comparten varios controladores.
 */
@Service
public class AccesoCurso {

    private final ICursoAlumno iCursoAlumno;

    public AccesoCurso(ICursoAlumno iCursoAlumno) {
        this.iCursoAlumno = iCursoAlumno;
    }

    /** El usuario es el maestro que imparte el curso. */
    public boolean esMaestro(Curso curso, Integer idUsuario) {
        return curso != null && idUsuario != null
                && curso.getMaestro() != null
                && curso.getMaestro().getIdUsuario().equals(idUsuario);
    }

    /** El usuario es un alumno inscrito en el curso. */
    public boolean esAlumnoInscrito(Curso curso, Integer idUsuario) {
        return curso != null && idUsuario != null
                && iCursoAlumno.existsById(new CursoAlumnoId(curso.getIdCurso(), idUsuario));
    }

    /** El usuario es el maestro o un alumno inscrito. */
    public boolean esMiembro(Curso curso, Integer idUsuario) {
        return esMaestro(curso, idUsuario) || esAlumnoInscrito(curso, idUsuario);
    }
}
