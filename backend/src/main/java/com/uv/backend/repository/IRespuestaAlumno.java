package com.uv.backend.repository;

import com.uv.backend.entity.RespuestaAlumno;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

/**
 * Repositorio Spring Data JPA para la entidad {@link RespuestaAlumno}.
 */
public interface IRespuestaAlumno extends CrudRepository<RespuestaAlumno, Integer> {

    /**
     * Lista las respuestas de un resultado de examen.
     *
     * @param idResultado identificador del resultado
     * @return respuestas del resultado
     */
    List<RespuestaAlumno> findByResultadoIdResultado(Integer idResultado);
}
