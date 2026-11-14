package com.uv.backend.repository;

import com.uv.backend.entity.CursoAlumno;
import com.uv.backend.entity.CursoAlumnoId;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

/**
 * Repositorio Spring Data JPA para la entidad {@link CursoAlumno}.
 */
public interface ICursoAlumno extends CrudRepository<CursoAlumno, CursoAlumnoId> {

    /**
     * Lista las inscripciones de un curso.
     *
     * @param idCurso identificador del curso
     * @return alumnos inscritos en el curso
     */
    List<CursoAlumno> findByCursoIdCurso(Integer idCurso);

    /**
     * Lista los cursos en los que está inscrito un alumno.
     *
     * @param idAlumno identificador del alumno
     * @return inscripciones del alumno
     */
    List<CursoAlumno> findByAlumnoIdUsuario(Integer idAlumno);
}
