package com.uv.backend.repository;

import com.uv.backend.entity.Tarea;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

/**
 * Repositorio Spring Data JPA para la entidad {@link Tarea}.
 */
public interface ITarea extends CrudRepository<Tarea, Integer> {

    /**
     * Lista las tareas de un curso.
     *
     * @param idCurso identificador del curso
     * @return tareas del curso
     */
    List<Tarea> findByCursoIdCurso(Integer idCurso);
}
