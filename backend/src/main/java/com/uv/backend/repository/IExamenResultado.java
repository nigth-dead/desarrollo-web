package com.uv.backend.repository;

import com.uv.backend.entity.ExamenResultado;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

/**
 * Repositorio Spring Data JPA para la entidad {@link ExamenResultado}.
 */
public interface IExamenResultado extends CrudRepository<ExamenResultado, Integer> {

    /**
     * Lista los resultados de un examen.
     *
     * @param idExamen identificador del examen
     * @return resultados del examen
     */
    List<ExamenResultado> findByExamenIdExamen(Integer idExamen);

    /**
     * Busca el resultado de un alumno en un examen.
     *
     * @param idExamen identificador del examen
     * @param idAlumno identificador del alumno
     * @return el resultado si existe
     */
    Optional<ExamenResultado> findByExamenIdExamenAndAlumnoIdUsuario(Integer idExamen, Integer idAlumno);
}
