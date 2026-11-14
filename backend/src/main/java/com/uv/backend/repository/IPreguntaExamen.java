package com.uv.backend.repository;

import com.uv.backend.entity.PreguntaExamen;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

/**
 * Repositorio Spring Data JPA para la entidad {@link PreguntaExamen}.
 */
public interface IPreguntaExamen extends CrudRepository<PreguntaExamen, Integer> {

    /**
     * Lista las preguntas de un examen.
     *
     * @param idExamen identificador del examen
     * @return preguntas del examen
     */
    List<PreguntaExamen> findByExamenIdExamen(Integer idExamen);
}
