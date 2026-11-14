package com.uv.backend.repository;

import com.uv.backend.entity.Examen;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

/**
 * Repositorio Spring Data JPA para la entidad {@link Examen}.
 */
public interface IExamen extends CrudRepository<Examen, Integer> {

    /**
     * Lista los exámenes de un curso.
     *
     * @param idCurso identificador del curso
     * @return exámenes del curso
     */
    List<Examen> findByCursoIdCurso(Integer idCurso);
}
